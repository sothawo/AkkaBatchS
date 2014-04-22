package com.sothawo.akkabatch.messages;

import java.io.Serializable;

/**
 * Message with the work to be done; sent from the Reader.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class DoWork implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Record-ID */
    private final Long recordId;

    /** Original csv value */
    private final String csvOriginal;

// --------------------------- CONSTRUCTORS ---------------------------

    public DoWork(Long recordId, String csvOriginal) {
        this.recordId = recordId;
        this.csvOriginal = csvOriginal;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Long getRecordId() {
        return recordId;
    }

    public String getCsvOriginal() {
        return csvOriginal;
    }
}
