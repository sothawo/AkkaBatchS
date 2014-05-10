package com.sothawo.akkabatch.scala

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
class AkkaBatchException(msg: String, cause: Throwable) extends Exception(msg, cause) {
  def this(msg:String) = this(msg, null)
  def this() = this(null)
}
