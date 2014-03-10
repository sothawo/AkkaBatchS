package com.sothawo.akkabatch;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Writer Actor.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class Writer extends UntypedActor {
// ------------------------------ FIELDS ------------------------------

    /**
     * Logger
     */
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ProcessRecord) {
            processRecord((ProcessRecord) message);
        } else {
            unhandled(message);
        }
    }

    /**
     * verarbeitet den nächsten Datensatz.
     *
     * @param processRecord Datensatz zum Schreiben.
     */
    private void processRecord(ProcessRecord processRecord) {
        log.info("Datensatz Nr. " + processRecord.getRecordId());
        // TODO: Dummycode
        sender().tell(new WorkDone(), getSelf());
    }
}
