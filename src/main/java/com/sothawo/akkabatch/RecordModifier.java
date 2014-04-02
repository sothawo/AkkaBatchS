package com.sothawo.akkabatch;

import akka.actor.ActorSelection;
import com.sothawo.akkabatch.messages.ProcessRecord;
import com.typesafe.config.ConfigException;

import java.text.MessageFormat;
import java.util.Random;

/**
 * RecordModifier Actor.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class RecordModifier extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    protected static final String CONFIG_DROPRATE = "simulation.recordModifier.droprate";

    private static Random random = new Random();
    /** der Writer */
    private ActorSelection writer;
    /** Ausfallrate */
    private int dropRatePerMille = 0;
    private long numProcessed;
    private long numDropped;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ProcessRecord) {
            boolean drop = false;
            if (dropRatePerMille > 0 && random.nextInt(1000) < dropRatePerMille) {
                drop = true;
            }
            if (!drop) {
                ProcessRecord in = (ProcessRecord) message;
                ProcessRecord out = new ProcessRecord(in.getRecordId(), in.getCsvOriginal(),
                                                      RecordProcessor.processRecord(in.getRecord()));
                writer.tell(out, getSelf());
                numProcessed++;
            } else {
                numDropped++;
            }
        } else {
            unhandled(message);
        }
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        try {
            dropRatePerMille = configApp.getInt(CONFIG_DROPRATE);
        } catch (ConfigException e) {
            log.error(e, CONFIG_DROPRATE);
        }

        numProcessed = 0;
        numDropped = 0;

        // Writer ist im Master
        String writerPath = configApp.getString("network.master.address") + configApp.getString("names.writerRef");
        log.info("Writer Path aus Konfiguration: " + writerPath);
        writer = context().actorSelection(writerPath);
        log.info(MessageFormat.format("sende Daten zu {0}, drop rate: {1} 0/00", writer.pathString(), dropRatePerMille));
    }

    @Override
    public void postStop() throws Exception {
        log.debug(MessageFormat.format("processed: {0}, dropped: {1}", numProcessed, numDropped));
        super.postStop();
    }
}
