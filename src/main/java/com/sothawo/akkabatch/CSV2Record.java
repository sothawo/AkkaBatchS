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
import com.sothawo.akkabatch.messages.*;

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
    /** der Aktor für den nächsten Schritt */
    private ActorRef recordModifier;
    /** GetWork Message */
    private GetWork getWork = new GetWork();

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof DoWork) {
            doWork((DoWork) message);
        } else if (message instanceof WorkAvailable) {
            reader.tell(getWork, getSelf());
        }
        unhandled(message);
    }

    /**
     * die eigentliche Verarbeitung.
     *
     * @param doWork
     */
    private void doWork(DoWork doWork) {
        recordModifier.tell(new ProcessRecord(doWork.getRecordId(), doWork.getCsvOriginal(),
                                              Record.fromLine(doWork.getCsvOriginal())), getSelf());
        reader.tell(getWork, getSelf());
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        reader = getContext().actorFor(configApp.getString("reader.refname"));
        log.debug(MessageFormat.format("hole Daten von {0}", reader.path()));

        reader.tell(new Register(), getSelf());

        recordModifier = getContext().actorFor(configApp.getString("recordModifier.refname"));
        log.debug(MessageFormat.format("Sende  Daten zu {0}", recordModifier.path()));
    }


}
