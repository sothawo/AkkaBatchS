/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch;

import akka.actor.ActorRef;
import com.sothawo.akkabatch.messages.Register;
import com.sothawo.akkabatch.messages.SendAgain;

import java.util.LinkedList;
import java.util.List;

/**
 * Reader-Aktor.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class Reader extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    /** Liste mit Workern */
    private List<ActorRef> workerList = new LinkedList<>();

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Register) {
            workerList.add(sender());
            log.info("Registrierung von " + sender().path());
        }
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
