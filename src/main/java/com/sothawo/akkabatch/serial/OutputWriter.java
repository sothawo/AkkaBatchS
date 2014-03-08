 package com.sothawo.akkabatch.serial;

/**
 * Schreibt die Daten in die Ausgabedatei.
 *
 * @author P.J.Meisch (pj.meisch@sothawo.com)
 */
public interface OutputWriter {
    /** öffnet die Ausgabedatei */
    void open(String filename) throws Exception;

    /** schliesst die Ausgabedatei */
    void close();

    /** schreibt eine Zeile in die Ausgabe */
    void write(String line);
}
