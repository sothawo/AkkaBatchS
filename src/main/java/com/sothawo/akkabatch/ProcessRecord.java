package com.sothawo.akkabatch;

import java.io.Serializable;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class ProcessRecord implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /**
     * Record-ID
     */
    private Long recordId = 0L;

    /**
     * Original csv Wert
     */

    private String csvOriginal = "";

    /**
     * der eigentliche Record
     */
    private Record record = null;

// --------------------------- CONSTRUCTORS ---------------------------

    public ProcessRecord(Long recordId, String csvOriginal, Record record) {
        this.recordId = recordId;
        this.csvOriginal = csvOriginal;
        this.record = record;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getCsvOriginal() {
        return csvOriginal;
    }

    public Record getRecord() {
        return record;
    }

    public Long getRecordId() {
        return recordId;
    }
}
