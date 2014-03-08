package com.sothawo.akkabatch.serial;

import java.io.PrintWriter;

/**
 * schreibt in eine Datei.
 *
 * @author P.J.Meisch (pj.meisch@sothawo.com)
 */
public class FileOutputWriter implements OutputWriter {
// ------------------------------ FIELDS ------------------------------

    /** zum Schreiben */
    private PrintWriter writer;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface OutputWriter ---------------------

    @Override
    public void open(String filename) throws Exception {
        writer = new PrintWriter(filename, "iso-8859-1");
    }

    @Override
    public void close() {
        if (null != writer) {
            writer.flush();
            writer.close();
        }
    }

    @Override
    public void write(String line) {
        writer.println(line);
    }
}
