package com.sothawo.akkabatch;

import java.math.BigInteger;
import java.text.MessageFormat;

/**
 * Klasse, welche in einer statischen Methode die nte Fibonaccizahl berechnet
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class Fibonacci {
    /**
     * erechnet die n-te Fibnacci-Zahl
     *
     * @param n
     * @return Fibbonaccizahl
     */
    public static BigInteger calculate(int n) throws IllegalArgumentException {
        if (0 >= n) {
            throw new IllegalArgumentException();
        }
        if(n < 3) {
            return BigInteger.ONE;
        }
        BigInteger fib = BigInteger.ONE;
        BigInteger fibPrev = BigInteger.ONE;
        BigInteger fibTmp;
        int i = 2;
        while(i < n) {
            fibTmp = fibPrev.add(fib);
            fibPrev = fib;
            fib = fibTmp;
            i++;
        }
        return fib;
    }

    public static void main(String[] args) {
        for(int i = 0; i < 1000; i++) {
            long startTime = System.currentTimeMillis();
            BigInteger fib = Fibonacci.calculate(5000);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println(MessageFormat.format("{0}: {1} ms", i, duration));
        }
    }
}
