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
 * Basisklasse für die Aktoren des Systems. Stellt Logger und Konfiguration bereit.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public abstract class AkkaBatchActor extends UntypedActor {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /** das globale Konfigurationsobjekt */
    protected Config configAll;
    /** die Unterkonfiguration der Anwendung */
    protected Config configApp;

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public void preStart() throws Exception {
        super.preStart();
        configAll = context().system().settings().config();
        configApp = configAll.getConfig("com.sothawo.akkabatch");
    }
}
