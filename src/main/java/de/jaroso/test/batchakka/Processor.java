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
 * Ein Processor nimmt die Daten zur Verarbeitung entgegen, verarbeitet sie und gibt die verarbeiteten Daten an einen
 * OutputWriter in der richtigen Reihenfolge wieder aus.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public abstract class Processor {
// ------------------------------ FIELDS ------------------------------

    /** der writer für die Ausgabe */
    protected final OutputWriter writer;

// --------------------------- CONSTRUCTORS ---------------------------

    /** Konstruktor mit OutputWriter, setzt das field writer */
    public Processor(OutputWriter writer) {
        if (null == writer) {
            throw new IllegalArgumentException("kein writer");
        }
        this.writer = writer;
    }

    /**
     * verarbeitet die nächste Eingabezeile
     *
     * @param line
     *         Eingabedaten
     */
    public abstract void process(String line);
}
