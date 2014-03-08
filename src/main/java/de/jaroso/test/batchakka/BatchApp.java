/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package de.jaroso.test.batchakka;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

/**
 * Batch-Applikationsklasse.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class BatchApp {
// ------------------------------ FIELDS ------------------------------

    /** Name der Eingabedatei */
    private final String infileName;
    /** Name der Ausgabedatei */
    private final String outfileName;
    /** Flag ob Verarbeitung mit akka */
    private boolean useAkka = false;
    /** Anzahl Akka-Worker */
    private Integer numWorker = 0;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * @param args
     *         Programmargumente.
     */
    public BatchApp(String[] args) {
        if (null == args || args.length < 3) {
            throw new IllegalArgumentException("falsche Anzahl Parameter");
        }
        infileName = args[0];
        outfileName = args[1];
        if ("akka".equals(args[2])) {
            useAkka = true;
        }
        if (useAkka) {
            if (args.length < 4) {
                throw new IllegalArgumentException("falsche Anzahl Parameter");
            }
            numWorker = Integer.getInteger(args[3]);
        }
    }

// --------------------------- main() method ---------------------------

    /**
     * Main Methode
     *
     * @param args
     *         Programmargumente
     */
    public static void main(String[] args) {
        try {
            new BatchApp(args).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * führt die komplette Verarbeitung durch
     */
    private void run() throws Exception {
        // Eingabe
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(infileName), "iso-8859-1"));

        // Ausgabe
        OutputWriter writer = new FileOutputWriter();
        writer.open(outfileName);

        // Processor    
        Processor processor = useAkka ? null : new SerialProcessor(writer);
        if (null == processor) {
            throw new IllegalArgumentException("kein Processor vorhanden");
        }

        // sonstige Initialisierung
        long numRecords = 0;
        long startTime = System.currentTimeMillis();

        // Verarbeitung
        String line = reader.readLine();
        while (null != line) {
            numRecords++;
            processor.process(line);
            line = reader.readLine();
        }

        //Auswertung
        long endTime = System.currentTimeMillis();
        writer.close();
        System.out.println(MessageFormat.format("Dauer: {0} ms, {1} Sätze", endTime - startTime, numRecords));
    }
}
