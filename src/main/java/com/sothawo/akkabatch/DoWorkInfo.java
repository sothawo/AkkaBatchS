/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch;

import com.sothawo.akkabatch.messages.DoWork;

/**
 * Information über eine DoWork Nachricht in der Verarbeitung.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class DoWorkInfo {
// ------------------------------ FIELDS ------------------------------

    /** die DoWork Nachricht */
    private final DoWork doWork;
    /** Timestamp des letzten Sendevorgangs */
    private long timestamp;
    /** Anzahl bisheriger Sendeversuche */
    private int sendCount;

// --------------------------- CONSTRUCTORS ---------------------------

    public DoWorkInfo(DoWork doWork) {
        this.doWork = doWork;
        this.timestamp = System.currentTimeMillis();
        this.sendCount = 1;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public long getTimestamp() {
        return timestamp;
    }
}
