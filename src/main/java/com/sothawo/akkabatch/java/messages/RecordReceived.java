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
 * Message from the Writer to the Reader that data was received.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class RecordReceived {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Id of the received record */
    private final long id;

// --------------------------- CONSTRUCTORS ---------------------------

    public RecordReceived(long id) {
        this.id = id;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public long getId() {
        return id;
    }
}
