package com.sothawo.akkabatch;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * Applikationsklasse für die Verarbeitung mit Akka.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class BatchApp {
// ------------------------------ FIELDS ------------------------------

    /**
     * Logger
     */
    protected LoggingAdapter log;
    /**
     * Konfiguration der Applikation
     */
    private Config configApp;
    /**
     * das Inbox Objekt des Akka Systems
     */
    private Inbox inbox;

// --------------------------- main() method ---------------------------

    /**
     * @param args Programmargumente
     */
    public static void main(String[] args) {
        try {
            new BatchApp().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Programm als Methode der BatchApp-Klasse.
     */
    private void run() {
        // Konfiguration laden, das macht theoretisch ActorSystem auch, aber so verwenden wir die gleiche Konfiguration
        Config configAll = ConfigFactory.load();
        configApp = configAll.getConfig("com.sothawo.akkabatch");

        // Aktorensystem anlegen
        ActorSystem system = ActorSystem.create(configApp.getString("akka.system.name"), configAll);
        log = Logging.getLogger(system, this);
        inbox = Inbox.create(system);

        String csv = "460332901~1~WOLFGANG~STEINBERG~76133~KARLSRUHE~INNENSTADT-OST~ADLERSTR.~10~";
        ProcessRecord processRecord = new ProcessRecord(4711L, csv, Record.fromLine(csv));

        ActorRef writer = system.actorOf(Props.create(Writer.class), configApp.getString("writer.name"));
        inbox.send(writer, processRecord);

        long startTime = System.currentTimeMillis();

        // an die eigene Inbox eine Message in 15 Sekunden
//        system.scheduler()
//              .scheduleOnce(Duration.create(configApp.getLong("run.duration"), TimeUnit.SECONDS), inbox.getRef(),
//                            new String("shutdown"),
//                            system.dispatcher(), inbox.getRef());
        // jede Message an die Inbox fährt das System herunter, spätestens nach 24 Stunden

        // TODO: Dummy, der Writer schickt WorkDone
        Object msg = inbox.receive(Duration.create(24, TimeUnit.HOURS));
        if (msg instanceof WorkDone) {
            log.info("work done");
        } else {
            log.error("unbekannte Nachricht: " + msg.getClass().getCanonicalName());
        }
        //Auswertung
        long endTime = System.currentTimeMillis();
        System.out.println(MessageFormat.format("Dauer: {0} ms, {1} Sätze", endTime - startTime, 0));
        system.shutdown();
    }
}
