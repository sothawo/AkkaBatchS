package com.sothawo.akkabatch.scala

import scala.beans.BeanProperty

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */

object Record {
  def apply(line: String) = {
    if (null == line) {
      throw new IllegalArgumentException("record from null")
    }

    val fields: Array[String] = line.split("~", -1)
    if (fields.length != 10) {
      throw new IllegalArgumentException(s"invalid data: $line")
    }

    new Record(fields(0), fields(1), fields(2), fields(3), fields(4), fields(5), fields(6), fields(7),
      fields(8) + fields(9))
  }
}

case class Record(@BeanProperty id: String, @BeanProperty sex: String, @BeanProperty firstname: String,
                  @BeanProperty lastname: String, @BeanProperty zip: String, @BeanProperty city: String,
                  @BeanProperty district: String, @BeanProperty street: String, @BeanProperty number: String)


