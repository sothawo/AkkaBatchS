package com.sothawo.akkabatch.serial;

import java.io.PrintWriter;

/**
 * schreibt in eine Datei.
 *
 * @author P.J.Meisch (pj.meisch@sothawo.com)
 */
public class FileOutputWriter {
// ------------------------------ FIELDS ------------------------------

    /** zum Schreiben */
    private PrintWriter writer;

// -------------------------- OTHER METHODS --------------------------

    public void close() {
        if (null != writer) {
            writer.flush();
            writer.close();
        }
    }

    public void open(String filename) throws Exception {
        writer = new PrintWriter(filename, "iso-8859-1");
    }

    public void write(String line) {
        writer.println(line);
    }
}
