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

import java.text.MessageFormat;

/**
 * Aktor zum Erzeugen eines Record aus einer csv Zeile.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class CSV2Record extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    /** der Reader */
    private ActorRef reader;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        unhandled(message);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        reader = context().actorFor(configApp.getString("reader.ref.name"));
        log.debug(MessageFormat.format("hole Daten zu {0}", reader.path()));
    }
}
