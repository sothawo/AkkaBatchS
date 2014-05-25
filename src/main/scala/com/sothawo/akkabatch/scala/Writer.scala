package com.sothawo.akkabatch.scala

import java.io.PrintWriter


import akka.actor.{ActorSelection, Props}
import com.sothawo.akkabatch.scala.messages._
import scala.collection.mutable
import scala.collection.immutable.TreeMap

/**
 * Writer Actor.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class Writer extends AkkaBatchActor {
  /** to write the output */
  private var writer: PrintWriter = null

  /** Reader-actor */
  private var reader: ActorSelection = null

  /** mapping from record id to actual data as a tree map to have the recordIds ordering */
  private var outputData = TreeMap[Long, ProcessRecord]()

  /** recordId of the next record in the output file */
  private var nextRecordId: Long = 0L

  override def preStart() {
    super.preStart()
    reader = context.actorSelection(appConfig.getString("names.readerRef"))
    log.debug(s"sending infos to ${reader.pathString}")
  }

  override def postStop() {
    if (null != writer) {
      writer.flush()
      writer.close()
      writer = null
    }
    super.postStop()
  }

  def receive = {
    case msg: InitWriter => initWriter(msg)
    case msg: ProcessRecord => processRecord(msg)
  }

  /**
   * initializes the writer
   * @param msg init message
   */
  private def initWriter(msg: InitWriter) = {
    var result = true

    try {
      writer = new PrintWriter(msg.outputFilename, msg.encoding)
    }
    catch {
      case e: Exception => {
        log.error(e, "Initializing Writer")
        result = false
      }
    }

    outputData = TreeMap[Long, ProcessRecord]()
    nextRecordId = 1
    log.info(s"file: ${msg.outputFilename}, encoding: ${msg.encoding}, Init-result: ${result}")

    sender ! InitResult(result)
  }

  private def processRecord(msg: ProcessRecord) = {
    val recordId = msg.recordId
    // if the id has already been processed, ignore it, has been resend by the Reader too often.
    if (recordId >= nextRecordId) {
      reader ! RecordReceived(recordId)
      outputData += (recordId -> msg)
      var recordsWritten = 0L
      while (!outputData.isEmpty && nextRecordId == outputData.firstKey) {
        val record = outputData(nextRecordId) // existst as ist is firstkey
        outputData -= nextRecordId
        writer.println(record.csvOriginal)
        recordsWritten += 1
        nextRecordId += 1
        if (0 == (nextRecordId % 10000)) log.debug(s"processed: ${nextRecordId}")
    }
      if (recordsWritten > 0) {
        reader ! RecordsWritten(recordsWritten)
      }
    }
  }
}

object Writer {
  def props() = Props(new Writer())
}
