/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch.java.messages;

/**
 * Message to initialize the Reader.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class InitReader {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Name of the input file */
    private final String inputFilename;

    /** Encoding of the input file */
    private final String encoding;

// --------------------------- CONSTRUCTORS ---------------------------

    public InitReader(final String inputFilename, final String encoding) {
        this.inputFilename = inputFilename;
        this.encoding = encoding;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getInputFilename() {
        return inputFilename;
    }

    public String getEncoding() {
        return encoding;
    }
}
