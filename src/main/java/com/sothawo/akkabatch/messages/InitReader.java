/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch.messages;

/**
 * Nachricht zur Initialisierung des Readers.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class InitReader {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;
    /** Name der Eingabedatei */
    private String inputFilename;
    /** Encoding der Datei */
    private String encoding;

// --------------------------- CONSTRUCTORS ---------------------------

    public InitReader(String inputFilename, String encoding) {
        this.inputFilename = inputFilename;
        this.encoding = encoding;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getEncoding() {
        return encoding;
    }

    public String getInputFilename() {
        return inputFilename;
    }
}
