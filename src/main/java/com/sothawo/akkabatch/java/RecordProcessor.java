package com.sothawo.akkabatch.java;

import com.sothawo.akkabatch.scala.Record;

import java.util.Random;

/**
 * Dummy record processing. creates a copy and uses some time.
 */
public final class RecordProcessor {
// ------------------------------ FIELDS ------------------------------

    public static int fibonacci = 1000;
    public static int threadsleep = 10;
    private static Random rand = new Random();

// -------------------------- STATIC METHODS --------------------------

    public static Record processRecord(final Record origin) {
        if (null == origin) {
            throw new IllegalArgumentException("null input");
        }
        Record record =
                new Record(origin.getId(), origin.getSex(), origin.getFirstname(), origin.getLastname(),
                           origin.getZip(), origin.getCity(), origin.getDistrict(), origin.getStreet(),
                           origin.getNumber());
        useTime();
        return record;
    }

    /**
     * use some time
     */
    public static void useTime() {
        if (0 == (rand.nextInt() % 2)) {
            try {
                Thread.sleep(threadsleep);
            } catch (InterruptedException ignored) {
                System.err.println("oops, interrupted");
            }
        } else {
            Fibonacci.calculate(fibonacci);
        }
    }
}
