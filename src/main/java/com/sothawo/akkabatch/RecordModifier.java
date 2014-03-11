package com.sothawo.akkabatch;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.text.MessageFormat;

/**
 * RecordModifier Actor.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class RecordModifier extends UntypedActor {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /** der Writer */
    private ActorRef writer;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ProcessRecord) {
            ProcessRecord in = (ProcessRecord) message;
            ProcessRecord out = new ProcessRecord(in.getRecordId(), in.getCsvOriginal(),
                                                  Record.processRecord(in.getRecord()));
            writer.tell(out, getSelf());
        } else {
            unhandled(message);
        }
    }

    @Override
    public void preStart() {
        String writerName = context().system().settings().config().getString("com.sothawo.akkabatch.writer.ref.name");
        writer = context().actorFor(writerName);
        log.debug(MessageFormat.format("sende Daten zu {0}", writer.path()));
    }
}
