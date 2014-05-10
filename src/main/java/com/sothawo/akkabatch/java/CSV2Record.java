/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch.java;

import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import com.sothawo.akkabatch.scala.Record;
import com.sothawo.akkabatch.scala.RecordProcessor;
import com.sothawo.akkabatch.scala.messages.*;
import scala.concurrent.duration.Duration;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * Actor to create a Record from a csv line
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class CSV2Record extends AkkaBatchActor {
// ------------------------------ FIELDS ------------------------------

    protected static final String REGISTER = "register";

    /**
     * actor for the next processing step
     */
    private ActorSelection recordModifier;

    /**
     * Register message
     */
    private final Register register = new Register();

    /**
     * GetWork message
     */
    private final GetWork getWork = new GetWork();

    /**
     * the Reader, where work is pulled from
     */
    private ActorSelection reader;

    /**
     * Scheduler object for the registration
     */
    private Cancellable registerSchedule;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof DoWork) {
            doWork((DoWork) message);
        } else if (message instanceof WorkAvailable) {
            sender().tell(getWork, getSelf());
        } else if (message.equals(REGISTER)) {
            reader.tell(register, getSelf());
        } else {
            unhandled(message);
        }
    }

    /**
     * does the work
     *
     * @param doWork
     *         work to be done
     */
    private void doWork(DoWork doWork) {
        // convert into a Record and send it off
        ProcessRecord message = new ProcessRecord(doWork.getRecordId(), doWork.getCsvOriginal(),
                Record.apply(doWork.getCsvOriginal()));

        recordModifier.tell(message, getSelf());

        // use some time
        RecordProcessor.useTime();

        // ask for more work
        sender().tell(getWork, getSelf());
    }

    @Override
    public void postStop() throws Exception {
        if (null != registerSchedule) {
            registerSchedule.cancel();
            registerSchedule = null;
        }
        super.postStop();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        // Reader is found on the master host
        String readerPath = appConfig.getString("network.master.address") + appConfig.getString("names.readerRef");
        log.info("Reader path from configuration: " + readerPath);
        reader = getContext().actorSelection(readerPath);

        // RecordModifier is in the same host
        recordModifier = getContext().actorSelection(appConfig.getString("names.recordModifierRef"));
        log.info(MessageFormat.format("get data from {0}, send data to {1}", reader.pathString(),
                recordModifier.pathString()));

        // repeated message to myself to re-register with the Reader
        registerSchedule = getContext().system().scheduler()
                .schedule(Duration.create(0, TimeUnit.SECONDS),
                        Duration.create(appConfig.getInt("times.registerIntervall"), TimeUnit.SECONDS),
                        getSelf(),
                        REGISTER,
                        getContext().dispatcher(), getSelf()
                );
    }
}
