package com.sothawo.akkabatch.scala

import akka.actor.ActorSelection
import com.typesafe.config.ConfigException
import com.sothawo.akkabatch.scala.messages.ProcessRecord
import scala.util.Random

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class RecordModifier extends AkkaBatchActor {

  protected final val CONFIG_DROPRATE: String = "simulation.recordModifier.droprate"

  /** the Writer */
  private var writer: ActorSelection = null

  /** drop rate per thousand */
  private var dropRatePerMille: Int = 0

  private var numProcessed: Long = _

  private var numDropped: Long = _

  override def preStart(): Unit = {
    super.preStart()

    try {
      dropRatePerMille = appConfig.getInt(CONFIG_DROPRATE)
    } catch {
      case e: ConfigException => log.error(e, CONFIG_DROPRATE)
    }

    numProcessed = 0
    numDropped = 0

    // Writer is in Master
    val writerPath: String = appConfig.getString("network.master.address") + appConfig.getString("names.writerRef")
    writer = context.actorSelection(writerPath)

    log.info(s"Writer path from configuration: $writerPath")
    log.info(s"sending data to ${writer.pathString}, drop rate: $dropRatePerMille 0/00")
  }

  override def postStop() {
    log.debug(s"processed: $numProcessed, dropped: $numDropped")
    super.postStop()
  }

  def receive = {
    case msg: ProcessRecord => {
      if (dropRatePerMille == 0 || Random.nextInt(1000) >= dropRatePerMille) {
        writer ! ProcessRecord(msg.recordId, msg.csvOriginal, RecordProcessor.processRecord(msg.record))
        numProcessed += 1
      } else {
        numDropped += 1
      }
    }
  }

}
