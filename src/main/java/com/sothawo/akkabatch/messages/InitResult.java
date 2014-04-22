package com.sothawo.akkabatch.messages;

import java.io.Serializable;

/**
 * Initialization finished message.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class InitResult implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;

    /** success flag */
    private final Boolean success;

// --------------------------- CONSTRUCTORS ---------------------------

    public InitResult(Boolean success) {
        this.success = success;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Boolean isSuccess() {
        return success;
    }
}
