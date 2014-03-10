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

    /** Logger */
    protected LoggingAdapter log;
    /** Name der Eingabedatei */
    private final String infileName;
    /** Name der Ausgabedatei */
    private final String outfileName;
    /** Konfiguration der Applikation */
    private Config configApp;
    /** das Aktorensystem */
    private ActorSystem system;
    /** das Inbox Objekt des Akka Systems */
    private Inbox inbox;
    /** der Writer */
    // TODO: wird der wirklich als field benötigt?
    private ActorRef writer;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * @param args
     *         Programmargumente
     */
    public BatchApp(String[] args) {
        if (null == args || args.length < 2) {
            throw new IllegalArgumentException("falsche Anzahl Parameter");
        }
        infileName = args[0];
        outfileName = args[1];
    }

// --------------------------- main() method ---------------------------

    /**
     * @param args
     *         Programmargumente
     */
    public static void main(String[] args) {
        try {
            new BatchApp(args).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Programm als Methode der BatchApp-Klasse.
     */
    private void run() throws AkkaBatchException {
        // Konfiguration laden, das macht theoretisch ActorSystem auch, aber so verwenden wir die gleiche Konfiguration
        Config configAll = ConfigFactory.load();
        configApp = configAll.getConfig("com.sothawo.akkabatch");

        initAkka(configAll);
        initWriter();

        long startTime = System.currentTimeMillis();

        // an die eigene Inbox eine Message in 15 Sekunden
//        system.scheduler()
//              .scheduleOnce(Duration.create(configApp.getLong("run.duration"), TimeUnit.SECONDS), inbox.getRef(),
//                            new String("shutdown"),
//                            system.dispatcher(), inbox.getRef());
        // jede Message an die Inbox fährt das System herunter, spätestens nach 24 Stunden


        // TODO: Dummy, der Writer schickt WorkDone
        String csv = "460332901~1~WOLFGANG~STEINBERG~76133~KARLSRUHE~INNENSTADT-OST~ADLERSTR.~10~";
        ProcessRecord processRecord = new ProcessRecord(4711L, csv, Record.fromLine(csv));
        inbox.send(writer, processRecord);
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

    /**
     * Initialisiert den Writer.
     */
    private void initWriter() throws AkkaBatchException {
        writer = system.actorOf(Props.create(Writer.class), configApp.getString("writer.name"));
        inbox.send(writer, new InitWriter(outfileName, configApp.getString("charset.outfile")));
        Object msg = inbox.receive(Duration.create(5, TimeUnit.SECONDS));
        if (msg instanceof InitResult) {
            InitResult initResult = (InitResult) msg;
            if (!initResult.getSuccess()) {
                throw new AkkaBatchException("Fehler bei der Initialisierung des Writer");
            }
        } else {
            throw new AkkaBatchException("unbekannte Antwort");
        }
    }

    private void initAkka(Config configAll) {
        system = ActorSystem.create(configApp.getString("akka.system.name"), configAll);
        log = Logging.getLogger(system, this);
        inbox = Inbox.create(system);
    }
}
