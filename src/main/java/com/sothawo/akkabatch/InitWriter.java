/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch;

/**
 * Nachricht zur Initialisierung des Writers.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class InitWriter {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;
    /** Name der Ausgabedatei */
    private String outputFilename;
    /** Encoding der Datei */
    private String encoding;

// --------------------------- CONSTRUCTORS ---------------------------

    public InitWriter(String outputFilename, String encoding) {
        this.outputFilename = outputFilename;
        this.encoding = encoding;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getOutputFilename() {
        return outputFilename;
    }

    public String getEncoding() {
        return encoding;
    }
}
