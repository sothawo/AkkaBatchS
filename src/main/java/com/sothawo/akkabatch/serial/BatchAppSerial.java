package com.sothawo.akkabatch.serial;

import com.sothawo.akkabatch.Record;
import com.sothawo.akkabatch.RecordProcessor;
import com.sothawo.akkabatch.messages.ProcessRecord;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.MessageFormat;

/**
 * Batch-Applikationsklasse.
 *
 * @author P.J.Meisch (pj.meisch@sothawo.com)
 */
public class BatchAppSerial {
// ------------------------------ FIELDS ------------------------------

    /** Name der Eingabedatei */
    private final String infileName;
    /** Name der Ausgabedatei */
    private final String outfileName;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * @param args
     *         Programmargumente.
     */
    public BatchAppSerial(String[] args) {
        if (null == args || args.length < 2) {
            throw new IllegalArgumentException("falsche Anzahl Parameter");
        }
        infileName = args[0];
        outfileName = args[1];
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
            new BatchAppSerial(args).run();
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
        PrintWriter writer = new PrintWriter(outfileName, "iso-8859-1");


        // sonstige Initialisierung
        long numRecords = 0;
        long startTime = System.currentTimeMillis();

        // Verarbeitung
        String line = reader.readLine();
        while (null != line) {
            numRecords++;
            ProcessRecord processRecord = new ProcessRecord(numRecords, line,
                                                            RecordProcessor.processRecord(Record.fromLine(line)));
            writer.println(processRecord.getCsvOriginal());
            if(0 == (numRecords % 10000)){
                System.out.println(MessageFormat.format("verarbeitet: {0}: ", numRecords));
            }
            line = reader.readLine();
        }

        //Auswertung
        long endTime = System.currentTimeMillis();
        writer.close();
        System.out.println(MessageFormat.format("Dauer: {0} ms, {1} Sätze", endTime - startTime, numRecords));
    }
}
