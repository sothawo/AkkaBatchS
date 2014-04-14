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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;

/**
 * base class for the actors. provides logger and configuration.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public abstract class AkkaBatchActor extends UntypedActor {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /** global configuration object */
    protected Config configAll;
    /** application configuration */
    protected Config configApp;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void preStart() throws Exception {
        super.preStart();
        configAll = context().system().settings().config();
        configApp = configAll.getConfig("com.sothawo.akkabatch");
    }
}
