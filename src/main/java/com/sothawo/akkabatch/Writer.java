package com.sothawo.akkabatch;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sothawo.akkabatch.messages.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.TreeMap;

/**
 * Writer Actor.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class Writer extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /** für die eigentliche Ausgabe */
    private PrintWriter writer;
    /** Reader-Aktor */
    private ActorRef reader;
    /** Puffer als TreeMap, da sortiert auf die Sätze zugegriffen werden muss */
    private TreeMap<Long, ProcessRecord> outputBuffer = new TreeMap<>();
    /** recordId des nächsten zu schriebenden Satzes */
    private long nextRecordId;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ProcessRecord) {
            processRecord((ProcessRecord) message);
        } else if (message instanceof InitWriter) {
            initWriter((InitWriter) message);
        } else {
            unhandled(message);
        }
    }

    /**
     * Initialisiert den Writer.
     *
     * @param message
     */
    private void initWriter(InitWriter message) {
        Boolean result = true;
        try {
            writer = new PrintWriter(message.getOutputFilename(), message.getEncoding());
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(e, "Initialisierung Writer");
            result = false;
        }
        outputBuffer.clear();
        nextRecordId = 1;
        log.info(MessageFormat.format("Datei: {0}, Zeichensatz: {1}, Init-Ergebnis: {2}", message.getOutputFilename(),
                                      message.getEncoding(), result));
        sender().tell(new InitResult(result), getSelf());
    }

    /**
     * verarbeitet den nächsten Datensatz.
     *
     * @param processRecord
     *         Datensatz zum Schreiben.
     */
    private void processRecord(ProcessRecord processRecord) {
        Long recordId = processRecord.getRecordId();
        reader.tell(new RecordReceived(recordId), getSelf());
        outputBuffer.put(recordId, processRecord);

        long recordsWritten = 0;
        while (!outputBuffer.isEmpty() && nextRecordId == outputBuffer.firstKey()) {
            ProcessRecord record = outputBuffer.remove(nextRecordId);
            writer.println(record.getCsvOriginal());
            recordsWritten++;
            nextRecordId++;
        }

        if (0 < recordsWritten) {
            reader.tell(new RecordsWritten(recordsWritten), getSelf());
        }
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        reader = getContext().actorFor(configApp.getString("names.readerRef"));
        log.debug(MessageFormat.format("sende Infos an {0}", reader.path()));
    }

    @Override
    public void postStop() throws Exception {
        if(null != writer) {
            writer.flush();
            writer.close();
            writer = null;
        }
        super.postStop();
    }
}
