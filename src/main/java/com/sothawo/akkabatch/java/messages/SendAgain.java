package com.sothawo.akkabatch.java.messages;

import java.io.Serializable;

/**
 * Message to the Reader to reprocess data that is still missing in the output.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public final class SendAgain implements Serializable {
// ------------------------------ FIELDS ------------------------------

    public static final long serialVersionUID = 42L;
}
