/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch;

import com.sothawo.akkabatch.messages.SendAgain;

/**
 * Reader-Aktor.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class Reader extends AkkaBatchActor {
// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof SendAgain) {
            resendMessages();
        } else {
            unhandled(message);
        }
    }

    /**
     * Versendet die bisher nicht beim Writer angekommenen Nachrichten noch ein mal.
     */
    private void resendMessages() {
        log.debug("Daten erneut versenden");
    }
}
