/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch;

import akka.actor.UntypedActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;
import com.typesafe.config.Config;

/**
 * base class for the actors. provides logger and configuration.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public abstract class AkkaBatchActor extends UntypedActor {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    protected final DiagnosticLoggingAdapter log = Logging.getLogger(this);

    /** global configuration object */
    protected Config globalConfig;

    /** application configuration */
    protected Config appConfig;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void preStart() throws Exception {
        super.preStart();
        globalConfig = context().system().settings().config();
        appConfig = globalConfig.getConfig("com.sothawo.akkabatch");
    }
}
