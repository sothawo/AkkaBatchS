package com.sothawo.akkabatch.messages;

import com.sothawo.akkabatch.Record;

import java.io.Serializable;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class ProcessRecord implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Record-ID */
    private final Long recordId;
    /** Original csv Wert */
    private final String csvOriginal;
    /** der eigentliche Record */
    private final Record record;

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
