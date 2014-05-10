package com.sothawo.akkabatch.scala

import com.sothawo.akkabatch.scala.messages.DoWork
import scala.beans.BeanProperty

/**
 * Information about a DoWork message during processing.
 *
 * @param doWork the DoWork message
 * @param timestamp Timestamp of the last send
 * @param sendCount number of sending operations
 */
class DoWorkInfo(@BeanProperty val doWork: DoWork, @BeanProperty var timestamp: Long,
var sendCount: Int ) {

  // TODO: ctor raus und default args im primary ctor, wenn kein Java mehr
  def this(doWork: DoWork)  { this(doWork, System.currentTimeMillis(), 1) }

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
