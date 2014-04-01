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
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    /**
     * aktuell zu verarbeitende Daten, in Map, damit bei einem resend nicht der gleiche Datensatz mehrfach verschickt
     * wird, explizit als TreeMap
     */
    private final TreeMap<Long, DoWork> workToBeDone = new TreeMap<>();

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

    /** Anzahl schon verarbeiteter, aber noch nicht geschriebener Sätze */
    private long numRecordsProcessed;

    /** durchschnittliche Verarbeitungszeit eines Datensatzes */
    private long averageProcessingTimeMs;

    /** Map mit Infos zu den aktuellen DoWork Nachrichten */
    private Map<Long, DoWorkInfo> doWorkInfos = new HashMap<>();

    /** die Message zum erneuten Senden */
    private SendAgain resend;

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
        } else if (message instanceof RecordReceived) {
            recordReceived((RecordReceived) message);
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
     *         die Nachricht
     */
    private void initReader(InitReader message) {
        inbox = sender();
        workAvailable = new WorkAvailable();
        maxNumRecordsInSystem = configApp.getLong("numRecords.inSystem");
        workToBeDone.clear();
        actNumRecordsInSystem = 0;
        recordSerialNo = 1;
        numRecordsInInput = 0;
        numRecordsInOutput = 0;
        numRecordsProcessed = 0;
        averageProcessingTimeMs = 0;
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
        } else {
            // resend Message schedulen
            resend = new SendAgain();
            // der erste resend nach 500ms, danach passt sich das Systen selber ann
            getContext().system().scheduler()
                        .scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS), getSelf(), resend,
                                      getContext().dispatcher(),
                                      null);
        }
    }

    /**
     * MessageHandler, entfernt den entsprechenden Datensatz aus der Map der eventuell neu zu versendenden Nachrichten
     * und aktualisiert die durchschnittliche Verarbeitungszeit.
     *
     * @param message
     *         enthält die Id des vom Writer empfangenen Datensatzes.
     */
    private void recordReceived(RecordReceived message) {
        Long recordId = message.getId();
        DoWorkInfo doWorkInfo = doWorkInfos.get(recordId);
        if (null != doWorkInfo) {
            // muss nicht mehr neu versendet werden
            doWorkInfos.remove(recordId);
            // auch die schon für den Versand vorbereiteten Einträge löschen
            workToBeDone.remove(recordId);

            // Verarbeitungszeit anpassen
            long duration = System.currentTimeMillis() - doWorkInfo.getTimestamp();
            if (0 == averageProcessingTimeMs) {
                averageProcessingTimeMs = duration;
            } else if (duration != averageProcessingTimeMs) {
                // TODO: evtl nur neu setzen, wenn Abweichung grösser Schwellwert
                averageProcessingTimeMs =
                        ((averageProcessingTimeMs * numRecordsProcessed) + duration) / (numRecordsProcessed + 1);
            }
            numRecordsProcessed++;
        }
    }

    /**
     * MessageHandler, lädt die nächsten Daten bzw. beendet die Verarbeitung
     *
     * @param message
     *         enthält die Anzahl der geschriebenen Daten
     * @throws IOException
     */
    private void recordsWritten(RecordsWritten message) throws IOException {
        Long numRecordsWritten = message.getNumRecords();
        actNumRecordsInSystem -= numRecordsWritten;
        fillWorkToBeDone();
        numRecordsInOutput += numRecordsWritten;
        if (null == reader && numRecordsInInput == numRecordsInOutput) {
            // alles fertig
            log.debug(MessageFormat.format("durchschn. Verarbeitungszeit: {0} ms", averageProcessingTimeMs));
            inbox.tell(new WorkDone(Boolean.TRUE), getSelf());
        }
    }

    /**
     * füllt den Puffer der zu verarbeitenden Daten und benachrichtigt die Worker, dass Arbeit da ist.
     */
    private void fillWorkToBeDone() throws IOException {
        boolean breakout = false;
        while (null != reader && actNumRecordsInSystem < maxNumRecordsInSystem && !breakout) {
            String line = reader.readLine();
            if (line != null) {
                long recordId = recordSerialNo++;
                workToBeDone.put(recordId, new DoWork(recordId, line));
                actNumRecordsInSystem++;
                numRecordsInInput++;
            } else {
                // keine weiteren Daten mehr
                reader.close();
                reader = null;
            }
            if (500 == numRecordsInInput) {
                // nach den ersten 500 erst nicht weitermachen, damit die Maschinerie schnell anlaufen kann
                // der Rest wird nach der Verarbeitung des ersten Satzes gemacht
                breakout = true;
            }
        }
        notifyWorkers();
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
        // Daten mit Timeout (doppelte durchschnittliche Zeit) nach workToBeDone verschieben
        long now = System.currentTimeMillis();
        long overdueTimestamp = now - (2 * averageProcessingTimeMs);
        for (Map.Entry<Long, DoWorkInfo> entry : doWorkInfos.entrySet()) {
            DoWorkInfo doWorkInfo = entry.getValue();
            if (doWorkInfo.getTimestamp() <= overdueTimestamp) {
                doWorkInfo.markResend(now);
                workToBeDone.put(entry.getKey(), doWorkInfo.getDoWork());
            }
        }
        notifyWorkers();

        // neue Message schedulen
        FiniteDuration interval = Duration.create(3 * averageProcessingTimeMs, TimeUnit.MILLISECONDS);
        getContext().system().scheduler().scheduleOnce(interval, getSelf(), resend, getContext().dispatcher(),
                                                       null);
    }

    /**
     * verschickt eine WorkAvailable Nachricht an alle registrierten Worker, wenn es etwas zu tun gibt
     */
    private void notifyWorkers() {
        if (0 < workToBeDone.size()) {
            for (ActorRef worker : workerList) {
                worker.tell(workAvailable, getSelf());
            }
        }
    }

    /**
     * schickt den nächsten Satz zur Verarbeitung an einen Worker.
     */
    private void sendWork() {
        if (0 < workToBeDone.size()) {
            DoWork doWork = workToBeDone.firstEntry().getValue();
            Long recordId = doWork.getRecordId();
            workToBeDone.remove(recordId);

            // nur in Info Map, wenn nicht schon drin
            if (!doWorkInfos.containsKey(recordId)) {
                doWorkInfos.put(recordId, new DoWorkInfo(doWork));
            }
            sender().tell(doWork, getSelf());
        }
    }
}
