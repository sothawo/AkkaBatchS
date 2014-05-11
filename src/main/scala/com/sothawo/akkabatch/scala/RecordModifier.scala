package com.sothawo.akkabatch.scala

import akka.actor.ActorSelection
import com.typesafe.config.ConfigException
import com.sothawo.akkabatch.scala.messages.ProcessRecord
import scala.util.Random

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class RecordModifier extends AkkabatchActor {

  protected final val CONFIG_DROPRATE: String = "simulation.recordModifier.droprate"

  /** the Writer */
  private var writer: ActorSelection = null

  /** drop rate per thousand */
  private var dropRatePerMille: Int = 0

  private var numProcessed: Long = _

  private var numDropped: Long = _

  override def preStart(): Unit = {
    super.preStart

    try {
      dropRatePerMille = appConfig.getInt(CONFIG_DROPRATE)
    } catch {
      case e: ConfigException => {
        log.error(e, CONFIG_DROPRATE)
      }
    }

    numProcessed = 0
    numDropped = 0

    // Writer ist im Master
    val writerPath: String = appConfig.getString("network.master.address") + appConfig.getString("names.writerRef")
    log.info(s"Writer path from configuration: $writerPath")
    writer = context.actorSelection(writerPath)
    val actWriterPath = writer.pathString
    log.info(s"sending data to $actWriterPath, drop rate: $dropRatePerMille 0/00")
  }

  override def postStop {
    log.debug(s"processed: $numProcessed, dropped: $numDropped")
    super.postStop
  }

  def receive = {
    case processRecordMsg: ProcessRecord => {
      if (dropRatePerMille == 0 || Random.nextInt(1000) >= dropRatePerMille) {
        writer ! ProcessRecord(processRecordMsg.getRecordId, processRecordMsg.getCsvOriginal,
          RecordProcessor.processRecord(processRecordMsg.getRecord))
        numProcessed += 1
      } else {
        numDropped += 1
      }
    }
  }

}
