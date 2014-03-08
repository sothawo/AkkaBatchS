/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package de.jaroso.test.batchakka;

/**
 * Schreibt die Daten in die Ausgabedatei.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public interface OutputWriter {
    /** öffnet die Ausgabedatei */
    void open(String filename) throws Exception;

    /** schliesst die Ausgabedatei */
    void close();

    /** schreibt eine Zeile in die Ausgabe */
    void write(String line);
}
