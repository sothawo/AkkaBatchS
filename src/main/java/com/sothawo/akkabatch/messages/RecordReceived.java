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
 * Nachricht, dass Daten vom Writer empfangen wurden.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public final class RecordReceived {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** Id des Datensatzes */
    private final Long id;

// --------------------------- CONSTRUCTORS ---------------------------

    public RecordReceived(Long id) {
        this.id = id;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Long getId() {
        return id;
    }
}
