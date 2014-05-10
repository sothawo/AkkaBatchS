package com.sothawo.akkabatch.scala.messages

import scala.beans.{BooleanBeanProperty, BeanProperty}

/**
 * the different messages, all in one file, as they are simple case classes
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */

case class InitResult(@BooleanBeanProperty success: Boolean)

case class Register()

case class WorkAvailable()

case class GetWork()

case class DoWork(@BeanProperty recordId: Long, @BeanProperty csvOriginal: String)

case class SendAgain()

case class WorkDone(@BooleanBeanProperty success: Boolean)

case class RecordReceived(@BeanProperty id: Long)

case class RecordsWritten(@BeanProperty numRecords: Long)
