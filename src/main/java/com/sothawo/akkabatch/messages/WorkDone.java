package com.sothawo.akkabatch.messages;

import java.io.Serializable;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class WorkDone implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;
    /** Erfolgsflag */
    private final Boolean success;

// --------------------------- CONSTRUCTORS ---------------------------

    public WorkDone(Boolean success) {
        this.success = success;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Boolean getSuccess() {
        return success;
    }
}
