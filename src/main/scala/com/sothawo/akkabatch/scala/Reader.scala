package com.sothawo.akkabatch.scala

import akka.actor.{Props, ActorRef, Cancellable}
import com.sothawo.akkabatch.scala.messages._
import com.sothawo.akkabatch.scala.messages.DoWork
import com.sothawo.akkabatch.scala.messages.InitReader
import com.sothawo.akkabatch.scala.messages.Register
import com.sothawo.akkabatch.scala.messages.WorkAvailable
import java.io.{IOException, FileInputStream, InputStreamReader, BufferedReader}
import java.util
import scala.concurrent.duration._


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
  private var reader: BufferedReader = _

  /** mapping of registered workers to the timestamp of the registration */
  private val workers: util.Map[ActorRef, Long] = new util.HashMap[ActorRef, Long]

  /** actual work to be done; put in a map, so when the work is rescheduled because of a timeout, it's not duplicated */
  private val workToBeDone: util.TreeMap[Long, DoWork] = new util.TreeMap[Long, DoWork]

  /** maximum number of records allowed in the system */
  private var maxNumRecordsInSystem: Long = _

  /** number of records in the system which cause a refill of the internal buffer */
  private var fillLevel: Long = _

  /** actual number of records in the system */
  private var actNumRecordsInSystem: Long = _

  /** recordId of the next record to read */
  private var recordSerialNo: Long = _

  /** number of records read fromm the input file */
  private var numRecordsInInput: Long = _

  /** number of records written to the output file */
  private var numRecordsInOutput: Long = _

  /** number of processed records, they must not be written out yet */
  private var numRecordsProcessed: Long = _

  /** average processing time of a record */
  private var averageProcessingTimeMs: Long = 0L

  /** map from recordIDs to DoWorkInfos */
  private val doWorkInfos: util.Map[Long, DoWorkInfo] = new util.HashMap[Long, DoWorkInfo]

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
    if (!workers.containsKey(sender)) {
      log.info("registered  " + sender.path)
    }
    workers.put(sender, System.currentTimeMillis)
    if (0 < workToBeDone.size) {
      sender ! workAvailable
    }
  }

  def initReader(msg: InitReader) {
    inbox = sender
    maxNumRecordsInSystem = appConfig.getLong("numRecords.inSystem")
    fillLevel = (maxNumRecordsInSystem * 9) / 10
    workToBeDone.clear()
    actNumRecordsInSystem = 0
    recordSerialNo = 1
    numRecordsInInput = 0
    numRecordsInOutput = 0
    numRecordsProcessed = 0
    averageProcessingTimeMs = 0

    var result: Boolean = true
    try {
      reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(msg.inputFilename), msg.encoding))
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
    if (0 < workToBeDone.size) {
      val doWork: DoWork = workToBeDone.firstEntry.getValue
      val recordId: Long = doWork.recordId
      workToBeDone.remove(recordId)
      if (!doWorkInfos.containsKey(recordId)) {
        doWorkInfos.put(recordId, new DoWorkInfo(doWork))
      }
      sender ! doWork
    }
  }

  def resendMessages() {
    // move data with timeout (twice the average processing time)
    val now: Long = System.currentTimeMillis
    val overdueTimestamp: Long = now - (2 * averageProcessingTimeMs)
    import scala.collection.JavaConversions._
    for (entry <- doWorkInfos.entrySet) {
      val doWorkInfo: DoWorkInfo = entry.getValue
      if (doWorkInfo.getTimestamp <= overdueTimestamp) {
        doWorkInfo.markResend(now)
        workToBeDone.put(entry.getKey, doWorkInfo.getDoWork)
      }
    }
    notifyWorkers()

    // check for resend in thrice the average processing time
    val interval = if (0 == averageProcessingTimeMs) 500 else averageProcessingTimeMs * 3
    context.system.scheduler.scheduleOnce(interval millis, self, resend)
  }

  def recordReceived(msg: RecordReceived) {
    val recordId: Long = msg.recordId
    val doWorkInfo: DoWorkInfo = doWorkInfos.get(recordId)


    if (null != doWorkInfo) {
      doWorkInfos.remove(recordId)
      workToBeDone.remove(recordId)
      val duration: Long = System.currentTimeMillis - doWorkInfo.getTimestamp
      if (0 == averageProcessingTimeMs) {
        averageProcessingTimeMs = duration
      }
      else if (duration != averageProcessingTimeMs) {
        averageProcessingTimeMs = ((averageProcessingTimeMs * numRecordsProcessed) + duration) / (numRecordsProcessed
          + 1)
      }
      if (1 > averageProcessingTimeMs) {
        averageProcessingTimeMs = 1
      }
      numRecordsProcessed += 1
    }
  }

  def recordsWritten(msg: RecordsWritten) {
    val numRecordsWritten: Long = msg.numRecords
    actNumRecordsInSystem -= numRecordsWritten
    fillWorkToBeDone()
    numRecordsInOutput += numRecordsWritten
    if (null == reader && numRecordsInInput == numRecordsInOutput) {
      log.debug(s"average processing time: ${averageProcessingTimeMs} ms")
      inbox ! WorkDone(true)
    }
  }

  def cleanupWorkers() {
    val cleanWorkers: util.Map[ActorRef, Long] = new util.HashMap[ActorRef, Long]
    // 5x registerInterval means timeout
    val staleTimestamp: Long = System.currentTimeMillis - (5000 * appConfig.getInt("times.registerIntervall"))
    import scala.collection.JavaConversions._
    for (entry <- workers.entrySet) {
      if (entry.getValue > staleTimestamp) {
        cleanWorkers.put(entry.getKey, entry.getValue)
      }
      else {
        log.debug("remove worker " + entry.getKey.path)
      }
    }
    workers.clear()
    workers.putAll(cleanWorkers)
  }

  def fillWorkToBeDone() {
    // only fill when there are less than the configured records in the system
    if (actNumRecordsInSystem < fillLevel) {
      var breakout: Boolean = false
      while (null != reader && actNumRecordsInSystem < maxNumRecordsInSystem && !breakout) {
        val line: String = reader.readLine
        if (line != null) {
          val recordId: Long = recordSerialNo;
          recordSerialNo += 1
          workToBeDone.put(recordId, DoWork(recordId, line))
          actNumRecordsInSystem += 1
          numRecordsInInput += 1
        }
        else {
          reader.close()
          reader = null
        }
        if (500 == numRecordsInInput) {
          breakout = true
        }
      }
      notifyWorkers()
    }
  }

  /**
   * notifies the workers if there is work to be done
   */
  private def notifyWorkers() {
    if (0 < workToBeDone.size()) {
      import scala.collection.JavaConversions._
      for (worker <- workers.keySet()) {
        worker ! workAvailable;
      }
    }
  }
}

object Reader {
  def props() = Props(new Reader())
}
