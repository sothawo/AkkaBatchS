package com.sothawo.akkabatch.scala

import java.text.MessageFormat
import akka.actor.{Cancellable, ActorSelection}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.sothawo.akkabatch.scala.messages._
import com.sothawo.akkabatch.scala.messages.GetWork
import com.sothawo.akkabatch.scala.messages.WorkAvailable
import com.sothawo.akkabatch.scala.messages.Register
import com.sothawo.akkabatch.scala.messages.DoWork

/**
 * Actor to create a Record from a csv line
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class CSV2Record extends AkkaBatchActor {

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

    val readerPath: String = appConfig.getString("network.master.address") + appConfig.getString("names.readerRef")
    reader = context.actorSelection(readerPath)
    recordModifier = context.actorSelection(appConfig.getString("names.recordModifierRef"))

    log.info("Reader path from configuration: " + readerPath)
    log.info(MessageFormat.format("get data from {0}, send data to {1}", reader.pathString, recordModifier.pathString))

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
