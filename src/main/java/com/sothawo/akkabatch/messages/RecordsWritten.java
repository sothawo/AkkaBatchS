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
 * Nachricht, dass Daten vom Writer geschrieben wurden.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class RecordsWritten {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Anzahl geschriebener Datensätze */
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
