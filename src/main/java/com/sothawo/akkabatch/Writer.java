package com.sothawo.akkabatch;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sothawo.akkabatch.messages.InitResult;
import com.sothawo.akkabatch.messages.InitWriter;
import com.sothawo.akkabatch.messages.ProcessRecord;
import com.sothawo.akkabatch.messages.WorkDone;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

/**
 * Writer Actor.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class Writer extends UntypedActor {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /** für die eigentliche Ausgabe */
    private PrintWriter writer;

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
        log.info("Datensatz Nr. " + processRecord.getRecordId());
        // TODO: Dummycode
        sender().tell(new WorkDone(), getSelf());
    }
}
