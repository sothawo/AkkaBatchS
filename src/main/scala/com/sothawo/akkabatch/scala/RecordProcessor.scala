package com.sothawo.akkabatch.scala

import scala.beans.BeanProperty
import scala.util.Random

/**
 * Dummy record processing. creates a copy and uses some time.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
object RecordProcessor {
  @BeanProperty var fibonacci: Int = 1000
  @BeanProperty var threadsleep: Int = 10

  def processRecord(origin: Record): Record = {
    if (null == origin) {
      throw new IllegalArgumentException("null input")
    }

    useTime
    origin.copy()
  }

  /**
   * use some time
   */
  def useTime {
    if (0 == (Random.nextInt % 2)) {
      try {
        Thread.sleep(threadsleep)
      }
      catch {
        case ignored: InterruptedException => {
          System.err.println("oops, interrupted")
        }
      }
    }
    else {
      Fibonacci.calculate(fibonacci)
    }
  }
}
