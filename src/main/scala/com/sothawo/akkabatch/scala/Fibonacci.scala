package com.sothawo.akkabatch.scala

import java.math.BigInteger

/**
 * class to calculate the nth fibonacci-number.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
object Fibonacci {
  def calculate(n: Int): BigInteger = {
    if (0 >= n) {
      throw new IllegalArgumentException
    }

    var prev = BigInteger.ONE
    var fib = BigInteger.ONE
    var tmp = BigInteger.ZERO
    for (i <- 2 to n) {
      tmp = fib.add(prev)
      prev = fib
      fib = tmp
    }
    fib
  }
}
