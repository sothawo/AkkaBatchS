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
import com.sothawo.akkabatch.messages.InitReader;
import com.sothawo.akkabatch.messages.Register;
import com.sothawo.akkabatch.messages.SendAgain;
import com.sothawo.akkabatch.messages.WorkDone;

import java.io.*;
import java.text.MessageFormat;
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
    /** zum Lesen der Eingabedaten */
    private BufferedReader reader;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Register) {
            workerList.add(sender());
            log.info("Registrierung von " + sender().path());
        } else if (message instanceof InitReader) {
            initReader((InitReader) message);
        } else if (message instanceof SendAgain) {
            resendMessages();
        } else {
            unhandled(message);
        }
    }

    /**
     * Initialisiert den Reader und startet die Verarbeitung.
     *
     * @param message
     */
    private void initReader(InitReader message) {
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(message.getInputFilename()), message.getEncoding()));
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(e, "Initialisierung Reader");
            sender().tell(new WorkDone(false), getSelf());
        }
        log.info(MessageFormat.format("Datei: {0}, Zeichensatz: {1}", message.getInputFilename(),
                                      message.getEncoding()));
    }

    /**
     * Versendet die bisher nicht beim Writer angekommenen Nachrichten noch ein mal.
     */
    private void resendMessages() {
        log.debug("Daten erneut versenden");
    }
}
