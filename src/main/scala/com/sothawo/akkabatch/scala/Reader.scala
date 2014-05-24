package com.sothawo.akkabatch.scala

import java.io.IOException

import scala.collection.{mutable, Iterator}
import scala.concurrent.duration._
import scala.io.Source

import akka.actor.{ActorRef, Cancellable, Props}
import com.sothawo.akkabatch.scala.messages._

/**
 * Reader-actor.

 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class Reader extends AkkaBatchActor {
  protected val CLEANUP_WORKERS: String = "cleanupWorkers"

  /** implicit execution context for the scheduler */
  implicit val ec = context.dispatcher

  /** the Inbox of the system, for the last message at the end */
  private var inbox: ActorRef = _

  /** to read the input data */
  /** need to store the source to be able to close it */
  private var inputSource: Source = _
  private var inputIterator: Iterator[String] = _
  private var inputClosed = true

  /** mapping of registered workers to the timestamp of the registration */
  private var workers = new mutable.HashMap[ActorRef, Long]

  /** actual work to be done; put in a set, so when the work is rescheduled because of a timeout,
    * it's not duplicated. */
  private val workToBeDone = mutable.SortedSet[DoWork]()(DoWorkRecordIdOrdering)

  /** maximum number of records allowed in the system */
  private var maxNumRecordsInSystem: Long = _

  /** number of records in the system which cause a refill of the internal buffer */
  private var fillLevel: Long = _

  /** actual number of records in the system */
  private var actNumRecordsInSystem: Long = _

  /** recordId of the next record to read */
  private var recordSerialNo: Long = _

  /** number of records read fromm the input file */
  private var numRecordsFromInput: Long = _

  /** number of records written to the output file */
  private var numRecordsInOutput: Long = _

  /** number of processed records, they must not be written out yet */
  private var numRecordsProcessed: Long = _

  /** average processing time of a record */
  private var averageProcessingTimeMs: Long = 0L

  /** map from recordIDs to DoWorkInfos */
  private val workInProcess = new mutable.HashMap[Long, DoWorkInfo]

  /** message for resend */
  private val resend: SendAgain = SendAgain()

  /** Schedule to cleanup workers */
  private var cleanupWorkersSchedule: Cancellable = _

  /** instance of the WorkAvailable message */
  private val workAvailable: WorkAvailable = WorkAvailable()

  override def preStart() {
    super.preStart()
    cleanupWorkersSchedule = context.system.scheduler
      .schedule(0 seconds, (20 * appConfig.getInt("times.registerIntervall")) seconds, self, CLEANUP_WORKERS)
  }

  override def postStop() {
    if (null != cleanupWorkersSchedule) {
      cleanupWorkersSchedule.cancel()
      cleanupWorkersSchedule = null
    }
    super.postStop()
  }

  def receive = {
    case _: Register => registerWorker()
    case msg: InitReader => initReader(msg)
    case _: GetWork => sendWork()
    case _: SendAgain => resendMessages()
    case msg: RecordReceived => recordReceived(msg)
    case msg: RecordsWritten => recordsWritten(msg)
    case CLEANUP_WORKERS => cleanupWorkers()
  }

  def registerWorker() {
    if (!(workers contains sender)) log debug (s"registered  ${sender path}")
    workers(sender) = System.currentTimeMillis
    if (workToBeDone.size > 0) sender ! workAvailable
  }

  def initReader(msg: InitReader) {
    inbox = sender
    maxNumRecordsInSystem = appConfig.getLong("numRecords.inSystem")
    fillLevel = (maxNumRecordsInSystem * 9) / 10
    workToBeDone.clear()
    actNumRecordsInSystem = 0
    recordSerialNo = 1
    numRecordsFromInput = 0
    numRecordsInOutput = 0
    numRecordsProcessed = 0
    averageProcessingTimeMs = 0

    var result = true
    try {
      inputSource = Source.fromFile(msg inputFilename, msg encoding)
      inputIterator = inputSource getLines()
      inputClosed = false
      fillWorkToBeDone()
    }
    catch {
      case e: IOException => {
        log.error(e, "Initializing Reader")
        result = false
      }
    }

    log.info(s"file: ${msg.inputFilename}, encoding: ${msg.encoding}, Init-result: ${result}")

    if (!result) {
      sender ! WorkDone(false)
    }
    else {
      context.system.scheduler.scheduleOnce(500 millis, self, resend)
    }
  }

  def sendWork() {
    if (workToBeDone.size > 0) {
      val doWork = workToBeDone head
      val recordId = doWork.recordId
      workToBeDone -= doWork
      // only insert when not contained otherwise the timestamp is modified
      if (!workInProcess.contains(recordId)) workInProcess(recordId) = DoWorkInfo(doWork)
      sender ! doWork
    }
  }

  def resendMessages() {
    // move data with timeout (twice the average processing time)
    val now = System.currentTimeMillis
    val overdueTimestamp = now - (2 * averageProcessingTimeMs)

    for ((recordId, doWorkInfo) <- workInProcess) {
      if (doWorkInfo.timestamp <= overdueTimestamp) {
        doWorkInfo.markResend(now)
        workToBeDone += doWorkInfo.doWork
      }
    }

    // funktional:
    //    workInProcess foreach {
    //      case (recordId, doWorkInfo) => {
    //        if (doWorkInfo.timestamp <= overdueTimestamp) {
    //          doWorkInfo.markResend(now)
    //          workToBeDone.put(recordId, doWorkInfo.doWork)
    //        }
    //      }
    //    }
    notifyWorkers()

    // check for resend in thrice the average processing time
    val interval = if (0 == averageProcessingTimeMs) 500 else averageProcessingTimeMs * 3
    context.system.scheduler.scheduleOnce(interval millis, self, resend)
  }

  def recordReceived(msg: RecordReceived) {
    val recordId = msg.recordId
    if (workInProcess.contains(recordId)) {
      val doWorkInfo = workInProcess(recordId)
      workInProcess -= recordId
      workToBeDone -= doWorkInfo.doWork
      val duration = System.currentTimeMillis - doWorkInfo.timestamp
      if (0 == averageProcessingTimeMs) {
        averageProcessingTimeMs = duration abs
      }
      else if (duration != averageProcessingTimeMs) {
        averageProcessingTimeMs = (((averageProcessingTimeMs * numRecordsProcessed) + duration) / (numRecordsProcessed
          + 1)) max 1
      }
      numRecordsProcessed += 1
    }
  }

  def recordsWritten(msg: RecordsWritten) {
    val numRecordsWritten = msg.numRecords
    actNumRecordsInSystem -= numRecordsWritten
    fillWorkToBeDone()
    numRecordsInOutput += numRecordsWritten
    if (inputClosed && numRecordsFromInput == numRecordsInOutput) {
      log info (s"average processing time: ${averageProcessingTimeMs} ms")
      inbox ! WorkDone(true)
    }
  }

  def cleanupWorkers() {
    // 5x registerInterval means timeout
    val staleTimestamp = System.currentTimeMillis - (5000 * appConfig.getInt("times.registerIntervall"))
    //    workers = workers retain ((actor, timestamp) => if (timestamp > staleTimestamp) true else { log.debug
    // ("remove " +
    //      "worker " + actor.path); false})
    workers = workers retain ((_, timestamp) => timestamp > staleTimestamp)
  }

  def fillWorkToBeDone() {
    // only fill when there are less than the configured records in the system
    if (actNumRecordsInSystem < fillLevel) {
      while (!inputClosed && actNumRecordsInSystem < maxNumRecordsInSystem) {
        if (inputIterator hasNext) {
          val recordId = recordSerialNo;
          workToBeDone += DoWork(recordId, inputIterator.next())
          recordSerialNo += 1
          actNumRecordsInSystem += 1
          numRecordsFromInput += 1
        } else {
          inputSource.close()
          inputClosed = true
        }
      }
      notifyWorkers()
    }
  }

  /**
   * notifies the workers if there is work to be done
   */
  private def notifyWorkers() {
    if (workToBeDone.size > 0) workers.foreach(_._1 ! workAvailable)
  }
}

object Reader {
  def props() = Props(new Reader())
}
