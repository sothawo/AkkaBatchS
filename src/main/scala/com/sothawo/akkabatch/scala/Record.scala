package com.sothawo.akkabatch.scala

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */

/**
 * Record companion object.
 */
object Record {
  /**
   * creates a Record object from a csv line
   * @param line the csv line
   * @return the new Record
   */
  def apply(line: String) = {
    if (null == line) throw new IllegalArgumentException("record from null")

    val fields: Array[String] = line.split("~", -1)
    if (fields.length != 10) throw new IllegalArgumentException(s"invalid data: $line")

    new Record(fields(0), fields(1), fields(2), fields(3), fields(4), fields(5), fields(6), fields(7),
      fields(8) + fields(9))
  }
}

/**
 * Record case class
 * @param id
 * @param sex
 * @param firstname
 * @param lastname
 * @param zip
 * @param city
 * @param district
 * @param street
 * @param number
 */
case class Record(id: String, sex: String, firstname: String, lastname: String, zip: String, city: String,
                  district: String, street: String, number: String)


