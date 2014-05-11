package com.sothawo.akkabatch.scala

/**
 * class to calculate the nth fibonacci-number.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
object Fibonacci {
  def calculate(n: Int) = {
    if (0 >= n) {
      throw new IllegalArgumentException
    }

    var prev = BigInt(1)
    var fib = BigInt(1)
    var tmp = BigInt(0)
    for (i <- 2 to n) {
      tmp = prev + fib
      prev = fib
      fib = tmp
    }
    fib
  }
}
