package com.sothawo.akkabatch.scala

import scala.concurrent.duration._

import akka.actor.{ActorSelection, Cancellable, Props}
import com.sothawo.akkabatch.scala.messages._

/**
 * Actor to create a Record from a csv line
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class CSV2Record extends AkkaBatchActor {

  /** implicit execution context for the scheduler */
  implicit val ec = context.dispatcher

  private val REGISTER_RESEND = "registerResend"

  /** actor for the next processing step */
  private var recordModifier: ActorSelection = null

  /** the Reader, where work is pulled from */
  private var reader: ActorSelection = null

  /** Scheduler object for the registration */
  private var registerSchedule: Cancellable = null

  /** Register message */
  private val register = Register()

  /** GetWork message */
  private val getWork = GetWork()

  override def preStart() {
    super.preStart()

    val readerPath = appConfig.getString("network.master.address") + appConfig.getString("names.readerRef")
    reader = context.actorSelection(readerPath)
    recordModifier = context.actorSelection(appConfig.getString("names.recordModifierRef"))

    log.info("Reader path from configuration: " + readerPath)
    log.info(s"get data from ${reader.pathString}, send data to ${recordModifier.pathString}")

    // 0 seconds ist eigentlich 0.seconds und implizite Konvertierung aus scala.concurrent.duration._
    registerSchedule = context.system.scheduler
      .schedule(0 seconds, appConfig.getInt("times.registerIntervall") seconds, self, REGISTER_RESEND)
  }

  override def postStop() {
    if (null != registerSchedule) {
      registerSchedule.cancel()
      registerSchedule = null
    }
    super.postStop()
  }

  def receive = {
    case msg: DoWork => doWork(msg)
    case _: WorkAvailable => sender ! getWork
    case REGISTER_RESEND => reader ! register
  }

  /**
   * does the work
   *
   * @param msg work to be done
   */
  private def doWork(msg: DoWork) = {
    // convert into a Record and send it off in a ProcessRecord message
    recordModifier ! ProcessRecord(msg.recordId, msg.csvOriginal, Record(msg.csvOriginal))

    // use some time
    RecordProcessor.useTime

    // ask for more work
    sender ! getWork
  }
}

object CSV2Record {
  def props() = Props(new CSV2Record())
}
