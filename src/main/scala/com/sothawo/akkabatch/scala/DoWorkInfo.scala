package com.sothawo.akkabatch.scala

import com.sothawo.akkabatch.scala.messages.DoWork
import scala.beans.BeanProperty

/**
 * Information about a DoWork message during processing.
 *
 * @param doWork the DoWork message
 */
class DoWorkInfo(@BeanProperty val doWork: DoWork) {

  /** Timestamp of the last send */
  @BeanProperty var timestamp: Long = System.currentTimeMillis()

  /** number of sending operations */
  var sendCount: Int = 1

  /**
   * sets the internal timestamp value and increases the sendcount value
   *
   * @param timestamp new timestamp value
   */
  def markResend(timestamp: Long) {
    this.timestamp = timestamp
    sendCount += 1
  }
}
