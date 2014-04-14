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
import akka.actor.Cancellable;
import com.sothawo.akkabatch.messages.*;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Reader-actor.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class Reader extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    protected static final String CLEANUP_WORKERS = "cleanupWorkers";
    /** the Inbox of the system, for the last message at the end */
    private ActorRef inbox;

    /** to read the input data */
    private BufferedReader reader;

    /** instance of the WorkAvailable message */
    private WorkAvailable workAvailable;

    /** mappign of registered workers to the timestamp of the registration */
    private final Map<ActorRef, Long> workers = new HashMap<>();

    /**
     * actaul work to be done; put in a map, so when the work is rescheduled because of a timeout, it's not duplicated
     */
    private final TreeMap<Long, DoWork> workToBeDone = new TreeMap<>();

    /** maximum number of records allowed in the system */
    private long maxNumRecordsInSystem;

    /** number of records in the system which cause a refill of the internal buffer */
    private long fillLevel;

    /** actal number of records in the system */
    private long actNumRecordsInSystem;

    /** recordId of the next record to read */
    private long recordSerialNo;

    /** number of records read fromm the input file */
    private long numRecordsInInput;

    /** number of records written to the output file */
    private long numRecordsInOutput;

    /** number of processed records, they must not be written out yet */
    private long numRecordsProcessed;

    /** average processing time of a record */
    private long averageProcessingTimeMs;

    /** map from recordIDs to DoWorkInfos */
    private Map<Long, DoWorkInfo> doWorkInfos = new HashMap<>();

    /** mesage for resend */
    private SendAgain resend;

    /** Schedule to cleanup workers */
    private Cancellable cleanupWorkersSchedule;

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
        } else if (message.equals(CLEANUP_WORKERS)) {
            cleanupWorkers();
        } else {
            unhandled(message);
        }
    }

    /**
     * removes works which have not reregistered in time from the workers mapder workers Map
     */
    private void cleanupWorkers() {
        Map<ActorRef, Long> cleanWorkers = new HashMap<>();
        // 5x registerInterval means timeout
        long staleTimestamp = System.currentTimeMillis() - (5000 * configApp.getInt("times.registerIntervall"));
        for (Map.Entry<ActorRef, Long> entry : workers.entrySet()) {
            if (entry.getValue() > staleTimestamp) {
                cleanWorkers.put(entry.getKey(), entry.getValue());
            } else {
                log.debug("remove worker " + entry.getKey().path());
            }
        }
        workers.clear();
        workers.putAll(cleanWorkers);
    }

    /**
     * MessageHandler, initializes the Reader and starts processing
     *
     * @param message
     *         init message
     */
    private void initReader(InitReader message) {
        inbox = sender();
        workAvailable = new WorkAvailable();
        maxNumRecordsInSystem = configApp.getLong("numRecords.inSystem");
         fillLevel = (maxNumRecordsInSystem * 9) / 10;
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
            log.error(e, "Initializing Reader");
            result = false;
        }
        log.info(MessageFormat.format("file: {0}, encoding: {1}, Init-result: {2}", message.getInputFilename(),
                                      message.getEncoding(), result));
        if (!result) {
            sender().tell(new WorkDone(false), getSelf());
        } else {
            // resend Message schedulen
            resend = new SendAgain();
            // first resend after 500ms, after that the resend period is calculated dynamically
            getContext().system().scheduler()
                        .scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS), getSelf(), resend,
                                      getContext().dispatcher(),
                                      null);
        }
    }

    /**
     * removes the corrsepondign record from the map of potentially resendable records and updates the average processing time
     *
     * @param message
     *         contains the id of the processed record
     */
    private void recordReceived(RecordReceived message) {
        Long recordId = message.getId();
        DoWorkInfo doWorkInfo = doWorkInfos.get(recordId);
        if (null != doWorkInfo) {
            doWorkInfos.remove(recordId);
            workToBeDone.remove(recordId);

            long duration = System.currentTimeMillis() - doWorkInfo.getTimestamp();
            if (0 == averageProcessingTimeMs) {
                averageProcessingTimeMs = duration;
            } else if (duration != averageProcessingTimeMs) {
                averageProcessingTimeMs =
                        ((averageProcessingTimeMs * numRecordsProcessed) + duration) / (numRecordsProcessed + 1);
            }
            if(1 > averageProcessingTimeMs) {
                averageProcessingTimeMs = 1;
            }
            numRecordsProcessed++;
        }
    }

    /**
     * MessageHandler, loads new data
     *
     * @param message
     *         contains the number of written records
     * @throws IOException
     */
    private void recordsWritten(RecordsWritten message) throws IOException {
        Long numRecordsWritten = message.getNumRecords();
        actNumRecordsInSystem -= numRecordsWritten;
        fillWorkToBeDone();
        numRecordsInOutput += numRecordsWritten;
        if (null == reader && numRecordsInInput == numRecordsInOutput) {
            // all done
            log.debug(MessageFormat.format("average processing time: {0} ms", averageProcessingTimeMs));
            inbox.tell(new WorkDone(Boolean.TRUE), getSelf());
        }
    }

    /**
     * fills the buffer with records to process and notifies the workers
     */
    private void fillWorkToBeDone() throws IOException {
        // only fill when there are less than the configured records in the system
        if(actNumRecordsInSystem >= fillLevel) {
            return;
        }
        boolean breakout = false;
        while (null != reader && actNumRecordsInSystem < maxNumRecordsInSystem && !breakout) {
            String line = reader.readLine();
            if (line != null) {
                long recordId = recordSerialNo++;
                workToBeDone.put(recordId, new DoWork(recordId, line));
                actNumRecordsInSystem++;
                numRecordsInInput++;
            } else {
                // no more data
                reader.close();
                reader = null;
            }
            if (500 == numRecordsInInput) {
                // break after the first 500 records to get the system started fast
                breakout = true;
            }
        }
        notifyWorkers();
    }

    /**
     * registers the sender as worker and notifies if there is work
     */
    private void registerWorker() {
        ActorRef worker = sender();
        if (!workers.containsKey(worker)) {
            log.info("registered  " + worker.path());
        }
        workers.put(worker, System.currentTimeMillis());
        if (0 < workToBeDone.size()) {
            worker.tell(workAvailable, getSelf());
        }
    }

    /**
     * MessageHandler, resends the timed out records again
     */
    private void resendMessages() {
        // move data with timeout (twice the average processing time)
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

        // check for resend in thrice the average processing time
        FiniteDuration interval = Duration.create((0 == averageProcessingTimeMs) ? 500 : (3 * averageProcessingTimeMs),
                                                  TimeUnit.MILLISECONDS);
        getContext().system().scheduler().scheduleOnce(interval, getSelf(), resend, getContext().dispatcher(),
                                                       null);
    }

    /**
     * notifies the workers if there is work to be done
     */
    private void notifyWorkers() {
        if (0 < workToBeDone.size()) {
            for (ActorRef worker : workers.keySet()) {
                worker.tell(workAvailable, getSelf());
            }
        }
    }

    /**
     * sends the next work to the sender
     */
    private void sendWork() {
        if (0 < workToBeDone.size()) {
            DoWork doWork = workToBeDone.firstEntry().getValue();
            Long recordId = doWork.getRecordId();
            workToBeDone.remove(recordId);

            if (!doWorkInfos.containsKey(recordId)) {
                doWorkInfos.put(recordId, new DoWorkInfo(doWork));
            }
            sender().tell(doWork, getSelf());
        }
    }

    @Override
    public void postStop() throws Exception {
        if (null != cleanupWorkersSchedule) {
            cleanupWorkersSchedule.cancel();
            cleanupWorkersSchedule = null;
        }
        super.postStop();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        // regular cleanup message
        cleanupWorkersSchedule = getContext().system().scheduler().schedule(Duration.create(0, TimeUnit.SECONDS),
                                                                            Duration.create(20 * configApp
                                                                                                    .getInt("times.registerIntervall"),
                                                                                            TimeUnit.SECONDS
                                                                            ), getSelf(),
                                                                            CLEANUP_WORKERS,
                                                                            getContext().dispatcher(), getSelf()
        );
    }
}
