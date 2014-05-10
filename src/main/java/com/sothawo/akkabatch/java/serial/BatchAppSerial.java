package com.sothawo.akkabatch.java.serial;

import com.sothawo.akkabatch.java.RecordProcessor;
import com.sothawo.akkabatch.scala.Record;
import com.sothawo.akkabatch.scala.messages.ProcessRecord;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.MessageFormat;

/**
 * Batch-application. Serial, sequential, no Akka.
 *
 * @author P.J.Meisch (pj.meisch@sothawo.com)
 */
public class BatchAppSerial {
// ------------------------------ FIELDS ------------------------------

    /** name of the input file */
    private final String infileName;

    /** name of the output file */
    private final String outfileName;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * @param args
     *         program arguments
     */
    public BatchAppSerial(String[] args) {
        if (null == args || args.length < 2) {
            throw new IllegalArgumentException("wrong number of arguments");
        }
        infileName = args[0];
        outfileName = args[1];
    }

// --------------------------- main() method ---------------------------

    /**
     * main method
     *
     * @param args
     *         program arguments
     */
    public static void main(String[] args) {
        try {
            new BatchAppSerial(args).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * does the processing
     */
    private void run() throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(infileName), "iso-8859-1"));

        PrintWriter writer = new PrintWriter(outfileName, "iso-8859-1");

        long numRecords = 0;
        long startTime = System.currentTimeMillis();

        String line = reader.readLine();

        while (null != line) {
            numRecords++;
            ProcessRecord processRecord = new ProcessRecord(numRecords, line,
                    RecordProcessor.processRecord(Record.apply(line)));

            writer.println(processRecord.getCsvOriginal());
            if (0 == (numRecords % 10000)) {
                System.out.println(MessageFormat.format("processed: {0}: ", numRecords));
            }
            line = reader.readLine();
        }

        long endTime = System.currentTimeMillis();
        writer.close();
        System.out.println(MessageFormat.format("duration: {0} ms, {1} records", endTime - startTime, numRecords));
    }
}
