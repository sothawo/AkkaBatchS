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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

    /** die Inbox des Systems, für die letzte Meldung am Schluss */
    private ActorRef inbox;
    /** zum Lesen der Eingabedaten */
    private BufferedReader reader;
    /** eine Instanz der Nachricht reicht */
    private WorkAvailable workAvailable;
    /** Liste mit Workern */
    private final List<ActorRef> workerList = new LinkedList<>();
    /** aktuell zu verarbeitende Daten */
    private List<DoWork> workToBeDone = new LinkedList<>();
    /** maximale Anzahl gleichzeitg im System befindlicher Datensätze */
    private long maxNumRecordsInSystem;
    /** aktuelle Anzahl gleichzeitig im System befindlicher Datensätze */
    private long actNumRecordsInSystem;
    /** recordId des nächsten zu lesenden Satzes */
    private long recordSerialNo;
    /** Anzahl aus der Eingabedatei gelesener Sätze */
    private long numRecordsInInput;
    /** Anzahl in die Ausgabedatei geschriebener Sätze */
    private long numRecordsInOutput;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Register) {
            registerWorker();
        } else if (message instanceof InitReader) {
            initReader((InitReader) message);
        } else if (message instanceof GetWork) {
            sendWork();
        } else if (message instanceof SendAgain) {
            resendMessages();
        } else if (message instanceof RecordsWritten) {
            recordsWritten((RecordsWritten) message);
        } else {
            unhandled(message);
        }
    }

    /**
     * MessageHandler, initialisiert den Reader und startet die Verarbeitung.
     *
     * @param message
     */
    private void initReader(InitReader message) {
        inbox = sender();
        recordSerialNo = 1;
        numRecordsInInput = 0;
        numRecordsInOutput = 0;
        Boolean result = true;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(message.getInputFilename()), message.getEncoding()));
            fillWorkToBeDone();
        } catch (IOException e) {
            log.error(e, "Initialisierung Reader");
            result = false;
        }
        log.info(MessageFormat.format("Datei: {0}, Zeichensatz: {1}, Init-Ergebnis: {2}", message.getInputFilename(),
                                      message.getEncoding(), result));
        // Fehler zurückmelden
        if (!result) {
            sender().tell(new WorkDone(false), getSelf());
        }
    }

    /**
     * MessageHandler, lädt die nöchsten Daten bzw. beendet die Verarbeitung
     *
     * @param message
     *         enthält die Anzahl der geschriebenen Daten
     *
     * @throws IOException
     */
    private void recordsWritten(RecordsWritten message) throws IOException {
        Long numRecordsWritten = message.getNumRecords();
        actNumRecordsInSystem -= numRecordsWritten;
        fillWorkToBeDone();
        numRecordsInOutput += numRecordsWritten;
        if (null == reader && numRecordsInInput == numRecordsInOutput) {
            // alles fertig
            inbox.tell(new WorkDone(Boolean.TRUE), getSelf());
        }
    }

    /**
     * füllt den Puffer der zu verarbeitenden Daten und benachrichtigt die Worker, dass Arbeit da ist.
     */
    private void fillWorkToBeDone() throws IOException {
        while (null != reader && actNumRecordsInSystem < maxNumRecordsInSystem) {
            String line = reader.readLine();
            if (line != null) {
                workToBeDone.add(new DoWork(recordSerialNo++, line));
                actNumRecordsInSystem++;
                numRecordsInInput++;
            } else {
                // keine weiteren Daten mehr
                reader.close();
                reader = null;
            }
        }
        if (0 < workToBeDone.size()) {
            notifyWorkers();
        }
    }

    /**
     * verschickt eine WorkAvailable Nachricht an alle registrierten Worker.
     */
    private void notifyWorkers() {
        for (ActorRef worker : workerList) {
            worker.tell(workAvailable, getSelf());
        }
    }

    /**
     * registriert den Sender als Worker und gibt ihm evtl. gleich den Hinweis auf Arbeit
     */
    private void registerWorker() {
        workerList.add(sender());
        log.info("Registrierung von " + sender().path());
        if (0 < workToBeDone.size()) {
            sender().tell(workAvailable, getSelf());
        }
    }

    /**
     * MessageHandler, versendet die bisher nicht beim Writer angekommenen Nachrichten noch ein mal.
     */
    private void resendMessages() {
        log.debug("Daten erneut versenden");
        // TODO: Daten mit Timeout nach workToBeDone verschieben
    }

    /**
     * schickt den nächsten Satz zur Verarbeitung an einen Worker.
     */
    private void sendWork() {
        if (0 < workToBeDone.size()) {
            DoWork doWork = workToBeDone.get(0);
            workToBeDone.remove(0);
            sender().tell(doWork, getSelf());
        }
    }

    /**
     * Initialisierung vor dem Messagehandling
     *
     * @throws Exception
     */
    @Override
    public void preStart() throws Exception {
        super.preStart();
        workAvailable = new WorkAvailable();
        maxNumRecordsInSystem = configApp.getLong("numRecords.inSystem");
        workToBeDone.clear();
        actNumRecordsInSystem = 0;
        recordSerialNo = 0;
    }
}
