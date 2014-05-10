package com.sothawo.akkabatch.scala.messages

import scala.beans.{BeanProperty, BooleanBeanProperty}

import com.sothawo.akkabatch.scala.Record

/**
 * the different messages, all in one file, as they are simple case classes
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */

case class InitResult(@BooleanBeanProperty success: Boolean)

case class InitReader(@BeanProperty inputFilename: String , @BeanProperty encoding: String)

case class InitWriter(@BeanProperty outputFilename: String , @BeanProperty encoding: String)

case class Register()

case class WorkAvailable()

case class GetWork()

case class DoWork(@BeanProperty recordId: Long, @BeanProperty csvOriginal: String)

case class ProcessRecord(@BeanProperty recordId: Long, @BeanProperty csvOriginal: String, @BeanProperty record: Record)

case class SendAgain()

case class WorkDone(@BooleanBeanProperty success: Boolean)

case class RecordReceived(@BeanProperty recordId: Long)

case class RecordsWritten(@BeanProperty numRecords: Long)
