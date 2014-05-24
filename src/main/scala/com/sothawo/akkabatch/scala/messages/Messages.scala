package com.sothawo.akkabatch.scala.messages

import com.sothawo.akkabatch.scala.Record

/**
 * the different messages, all in one file, as they are simple case classes
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */

case class InitResult(success: Boolean)

case class InitReader(inputFilename: String , encoding: String)

case class InitWriter(outputFilename: String , encoding: String)

case class Register()

case class WorkAvailable()

case class GetWork()

case class DoWork(recordId: Long, csvOriginal: String)
object DoWorkRecordIdOrdering extends Ordering[DoWork]{
  def compare(a: DoWork, b: DoWork) = a.recordId compare b.recordId
}

case class ProcessRecord(recordId: Long, csvOriginal: String, record: Record)

case class SendAgain()

case class WorkDone(success: Boolean)

case class RecordReceived(recordId: Long)

case class RecordsWritten(numRecords: Long)
