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
 * Message from the Writer that a certain amount of data was written to the output.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class RecordsWritten {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** number of written records */
    private final Long numRecords;

// --------------------------- CONSTRUCTORS ---------------------------

    public RecordsWritten(Long numRecords) {
        this.numRecords = numRecords;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Long getNumRecords() {
        return numRecords;
    }
}
