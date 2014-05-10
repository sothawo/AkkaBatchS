package com.sothawo.akkabatch.java.messages;

import com.sothawo.akkabatch.java.Record;

import java.io.Serializable;

/**
 * Message containing the record to process.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class ProcessRecord implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Record-ID */
    private final Long recordId;

    /** Original csv line */
    private final String csvOriginal;

    /** Processed record instance */
    private final Record record;

// --------------------------- CONSTRUCTORS ---------------------------

    public ProcessRecord(final long recordId, final String csvOriginal, final Record record) {
        this.recordId = recordId;
        this.csvOriginal = csvOriginal;
        this.record = record;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Long getRecordId() {
        return recordId;
    }

    public String getCsvOriginal() {
        return csvOriginal;
    }

    public Record getRecord() {
        return record;
    }
}
