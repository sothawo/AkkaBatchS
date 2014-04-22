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
 * Message to initialize the Writer.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class InitWriter {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Name of the output file */
    private final String outputFilename;

    /** Encoding of the ouput file */
    private final String encoding;

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
