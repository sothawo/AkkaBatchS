package com.sothawo.akkabatch;

import java.util.Random;

/**
 * Datensatzverarbeitung. Liefert eine Kopie des Record und verbraucht etwas Zeit.
 */
public final class RecordProcessor {
// ------------------------------ FIELDS ------------------------------

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
        if (0 == (rand.nextInt() % 2)) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                System.err.println("oops, interrupted");
            }
        } else {
            Fibonacci.calculate(15000);
        }
        return record;
    }
}
