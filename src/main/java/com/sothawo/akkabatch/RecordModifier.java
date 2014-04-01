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

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ProcessRecord) {
            int randomValue = random.nextInt(1000);
            if(randomValue >= dropRatePerMille) {
                ProcessRecord in = (ProcessRecord) message;
                ProcessRecord out = new ProcessRecord(in.getRecordId(), in.getCsvOriginal(),
                                                      Record.processRecord(in.getRecord()));
                writer.tell(out, getSelf());
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

        if (0 < dropRatePerMille && dropRatePerMille < 1000) {
        }
        writer = context().actorSelection(configApp.getString("names.writerRef"));
        log.debug(MessageFormat.format("sende Daten zu {0}, drop rate: {1} 0/00", writer.path(), dropRatePerMille));
    }
}
