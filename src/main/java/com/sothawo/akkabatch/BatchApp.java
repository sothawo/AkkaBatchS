package com.sothawo.akkabatch;

import akka.actor.ActorSystem;
import akka.actor.Inbox;
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

    /** Konfiguration der Applikation */
    private Config configApp;

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
    private  void run() {
        // Konfiguration laden, das macht theoretisch ActorSystem auch, aber so verwenden wir die gleiche Konfiguration
        Config configAll = ConfigFactory.load();
        configApp = configAll.getConfig("com.sothawo.akkabatch");

        // Aktorensystem anlegen
        ActorSystem system = ActorSystem.create("AkkaBatch", configAll);

        // Inbox mit der auf die Shutdown Message gewartet wird
        Inbox inbox = Inbox.create(system);

//            ActorRef fileReader = system.actorOf(Props.create(FileConverter.class), "FileReader");

        long startTime = System.currentTimeMillis();

        // an die eigene Inbox eine Message in 15 Sekunden
        system.scheduler()
              .scheduleOnce(Duration.create(configApp.getLong("run.duration"), TimeUnit.SECONDS), inbox.getRef(),
                            new String("shutdown"),
                            system.dispatcher(), inbox.getRef());
        // jede Message an die Inbox fährt das System herunter, spätestens nach 24 Stunden
        Object msg = inbox.receive(Duration.create(24, TimeUnit.HOURS));

        //Auswertung
        long endTime = System.currentTimeMillis();
        System.out.println(MessageFormat.format("Dauer: {0} ms, {1} Sätze", endTime - startTime, 0));
        system.shutdown();
    }
}
