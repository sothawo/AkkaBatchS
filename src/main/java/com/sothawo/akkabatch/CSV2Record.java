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
import akka.actor.Cancellable;
import com.sothawo.akkabatch.messages.*;
import scala.concurrent.duration.Duration;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * Aktor zum Erzeugen eines Record aus einer csv Zeile.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class CSV2Record extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    protected static final String REGISTER = "register";
    /** der Aktor für den nächsten Schritt */
    private ActorSelection recordModifier;
    /** Register Message */
    private final Register register = new Register();
    /** GetWork Message */
    private final GetWork getWork = new GetWork();
    /** der Reader, bei dem Daten abgerufen werden */
    private ActorSelection reader;
    /** Scheduled Objekt für die Registrierung */
    private Cancellable registerSchedule = null;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof DoWork) {
            doWork((DoWork) message);
        } else if (message instanceof WorkAvailable) {
            sender().tell(getWork, getSelf());
        } else if (message.equals(REGISTER)) {
            reader.tell(register, getSelf());
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
        // in Record umwandeln und weiterschicken
        recordModifier.tell(new ProcessRecord(doWork.getRecordId(), doWork.getCsvOriginal(),
                                              Record.fromLine(doWork.getCsvOriginal())), getSelf());
        // neue Arbeit anfordern
        sender().tell(getWork, getSelf());
    }

    @Override
    public void postStop() throws Exception {
        if (null != registerSchedule) {
            registerSchedule.cancel();
            registerSchedule = null;
        }
        super.postStop();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        // Reader ist im Master
        String readerPath = configApp.getString("network.master.address") + configApp.getString("names.readerRef");
        log.info("Reader Path aus Konfiguration: " + readerPath);
        reader = getContext().actorSelection(readerPath);

        // recordModifier ist wie dieser Aktor im Worker
        recordModifier = getContext().actorSelection(configApp.getString("names.recordModifierRef"));
        log.info(MessageFormat.format("hole Daten von {0}, sende Daten zu {1}", reader.pathString(),
                                      recordModifier.pathString()));

        // zyklische Nachricht an das eigene Objekt mit einem String, um sich beim Reader zu registrieren
        // schöner wäre, den Scheduler gleich an den Reader schicken zu lassen, aber das ist mit einer ActorSelection
        // nicht möglich.
        registerSchedule = getContext().system().scheduler().schedule(Duration.create(0, TimeUnit.SECONDS),
                                                                      Duration.create(configApp
                                                                                              .getInt("times.registerIntervall"),
                                                                                      TimeUnit.SECONDS
                                                                      ), getSelf(),
                                                                      REGISTER,
                                                                      getContext().dispatcher(), getSelf()
        );
    }
}
