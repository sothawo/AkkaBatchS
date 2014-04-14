package com.sothawo.akkabatch;

import akka.actor.ActorSelection;
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

    /**
     * Logger
     */
    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /**
     * to write the output
     */
    private PrintWriter writer;
    /**
     * Reader-actor
     */
    private ActorSelection reader;
    /**
     * Buffer als TreeMap, because we need sorted access to the data
     */
    private final TreeMap<Long, ProcessRecord> outputBuffer = new TreeMap<>();
    /**
     * recordId of the next record in the output file
     */
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
     * processes the next record
     *
     * @param processRecord
     *         record to write
     */
    private void processRecord(ProcessRecord processRecord) {
        Long recordId = processRecord.getRecordId();
        // if the id has already been processed, ignore it, has been resend by the Reader too often.
        if (recordId >= nextRecordId) {
            reader.tell(new RecordReceived(recordId), getSelf());
            outputBuffer.put(recordId, processRecord);

            long recordsWritten = 0;
            while (!outputBuffer.isEmpty() && nextRecordId == outputBuffer.firstKey()) {
                ProcessRecord record = outputBuffer.remove(nextRecordId);
                writer.println(record.getCsvOriginal());
                recordsWritten++;
                nextRecordId++;
                if (0 == (nextRecordId % 10000)) {
                    log.debug(MessageFormat.format("processed: {0}", nextRecordId));
                }
            }

            if (0 < recordsWritten) {
                reader.tell(new RecordsWritten(recordsWritten), getSelf());
            }
        }
    }

    /**
     * Initializes the writer
     *
     * @param message
     *         init message
     */
    private void initWriter(InitWriter message) {
        Boolean result = true;
        try {
            writer = new PrintWriter(message.getOutputFilename(), message.getEncoding());
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(e, "Initializing Writer");
            result = false;
        }
        outputBuffer.clear();
        nextRecordId = 1;
        log.info(MessageFormat.format("file: {0}, encoding: {1}, Init-result: {2}", message.getOutputFilename(),
                                      message.getEncoding(), result));
        sender().tell(new InitResult(result), getSelf());
    }

    @Override
    public void postStop() throws Exception {
        if (null != writer) {
            writer.flush();
            writer.close();
            writer = null;
        }
        super.postStop();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        reader = getContext().actorSelection(configApp.getString("names.readerRef"));
        log.debug(MessageFormat.format("sending infos to {0}", reader.pathString()));
    }
}
