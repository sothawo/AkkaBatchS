/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch.java;

import com.sothawo.akkabatch.java.messages.DoWork;

/**
 * Information about a DoWork message during processing.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class DoWorkInfo {
// ------------------------------ FIELDS ------------------------------

    /** the DoWork message */
    private final DoWork doWork;

    /** Timestamp of the last send */
    private long timestamp;

    /** number of sending operations */
    private int sendCount;

// --------------------------- CONSTRUCTORS ---------------------------

    public DoWorkInfo(DoWork doWork) {
        this.doWork = doWork;
        this.timestamp = System.currentTimeMillis();
        this.sendCount = 1;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public DoWork getDoWork() {
        return doWork;
    }

    public long getTimestamp() {
        return timestamp;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * stes the internal timestamp value and increases the sendcount value
     *
     * @param timestamp
     */
    public void markResend(long timestamp) {
        this.timestamp = timestamp;
        sendCount++;
    }
}
