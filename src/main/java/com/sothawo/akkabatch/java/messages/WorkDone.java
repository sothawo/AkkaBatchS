package com.sothawo.akkabatch.java.messages;

import java.io.Serializable;

/**
 * Message that the work is done.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class WorkDone implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** success-flag */
    private final Boolean success;

// --------------------------- CONSTRUCTORS ---------------------------

    public WorkDone(final Boolean success) {
        this.success = success;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Boolean isSuccess() {
        return success;
    }
}
