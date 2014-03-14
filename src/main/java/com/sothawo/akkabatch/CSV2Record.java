/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch;

import akka.actor.ActorSelection;
import com.sothawo.akkabatch.messages.*;

import java.text.MessageFormat;

/**
 * Aktor zum Erzeugen eines Record aus einer csv Zeile.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class CSV2Record extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    /** der Aktor für den nächsten Schritt */
    private ActorSelection recordModifier;
    /** GetWork Message */
    private final GetWork getWork = new GetWork();

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof DoWork) {
            doWork((DoWork) message);
        } else if (message instanceof WorkAvailable) {
            sender().tell(getWork, getSelf());
        } else {
            unhandled(message);
        }
    }

    /**
     * die eigentliche Verarbeitung.
     *
     * @param doWork
     *         zu verarbeitende Daten
     */
    private void doWork(DoWork doWork) {
        recordModifier.tell(new ProcessRecord(doWork.getRecordId(), doWork.getCsvOriginal(),
                                              Record.fromLine(doWork.getCsvOriginal())), getSelf());
        sender().tell(getWork, getSelf());
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        ActorSelection reader = getContext().actorSelection(configApp.getString("names.readerRef"));
        log.debug(MessageFormat.format("hole Daten von {0}", reader.path()));
        reader.tell(new Register(), getSelf());

        recordModifier = getContext().actorSelection(configApp.getString("names.recordModifierRef"));
        log.debug(MessageFormat.format("Sende  Daten zu {0}", recordModifier.path()));
    }
}
