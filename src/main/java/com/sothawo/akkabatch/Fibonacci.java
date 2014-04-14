package com.sothawo.akkabatch;

import java.math.BigInteger;
import java.text.MessageFormat;

/**
 * class to calculate the nth fibonacci-number.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class Fibonacci {
// -------------------------- STATIC METHODS --------------------------

    public static BigInteger calculate(int n) throws IllegalArgumentException {
        if (0 >= n) {
            throw new IllegalArgumentException();
        }
        if (n < 3) {
            return BigInteger.ONE;
        }
        BigInteger fib = BigInteger.ONE;
        BigInteger fibPrev = BigInteger.ONE;
        BigInteger fibTmp;
        int i = 2;
        while (i < n) {
            fibTmp = fibPrev.add(fib);
            fibPrev = fib;
            fib = fibTmp;
            i++;
        }
        return fib;
    }
}
