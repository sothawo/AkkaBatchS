package com.sothawo.akkabatch;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.FromConfig;
import com.sothawo.akkabatch.messages.InitReader;
import com.sothawo.akkabatch.messages.InitResult;
import com.sothawo.akkabatch.messages.InitWriter;
import com.sothawo.akkabatch.messages.WorkDone;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import scala.concurrent.duration.Duration;

import java.io.File;
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
    /** Name der Konfigurationsdatei */
    private final String configFileName;
    /** Name der Eingabedatei */
    private String infileName = "not_specified";
    /** Name der Ausgabedatei */
    private String outfileName = "not_specified";
    /** Konfiguration der Applikation */
    private Config configApp;
    /** das Aktorensystem */
    private ActorSystem system;
    /** das Inbox Objekt des Akka Systems */
    private Inbox inbox;
    /** der Reader */
    private ActorRef reader;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * @param args
     *         Programmargumente
     */
    public BatchApp(String[] args) {
        if (null == args || args.length < 1) {
            throw new IllegalArgumentException("falscher Aufruf; Parameter: <config> [<infile> <outfile>]");
        }
        configFileName = args[0];
        if (args.length > 1) {
            infileName = args[1];
        }
        if (args.length > 2) {
            outfileName = args[2];
        }
    }

// --------------------------- main() method ---------------------------

    /**
     * @param args
     *         Programmargumente
     */
    public static void main(String[] args) {
        new BatchApp(args).run();
    }

    /**
     * Programmausführung als Methode der BatchApp-Klasse.
     */
    private void run() {
        try {
            System.out.println("#Cores: " + Runtime.getRuntime().availableProcessors());
            // Konfiguration aus Datei im Filesystem, nicht im Classpath
            Config configFile = ConfigFactory.parseFile(new File(configFileName));

            // application Konfiguration aus dem Classpath ohne resolving, das kommt nach dem merge
            ConfigParseOptions parseOptions = ConfigParseOptions.defaults();
            ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true);
            Config configAll = ConfigFactory.load("application", parseOptions, resolveOptions);
            // merge und dann resolve
            configAll = configFile.withFallback(configAll).resolve();

            configApp = configAll.getConfig("com.sothawo.akkabatch");

            initAkka(configAll);

            boolean runMaster = configApp.getBoolean("modules.master");
            boolean runWorker = configApp.getBoolean("modules.worker");
            log.info(MessageFormat.format("master: {0}, worker: {1}", runMaster, runWorker));

            if (runMaster) {
                initReader();
                initWriter();
            }
            if (runWorker) {
                initWorkers();
            }

            if (runMaster) {
                log.debug("Starte Verarbeitung...");
                // Verarbeitung durch Nachricht an den Reader starten
                inbox.send(reader, new InitReader(infileName, configApp.getString("charset.infile")));
            }
            waitForWorkDone();
        } catch (AkkaBatchException e) {
            e.printStackTrace();
        } finally {
            if (null != system) {
                system.shutdown();
            }
        }
    }

    /**
     * Initialisiert das Akka System und den Logger.
     *
     * @param config
     *         KOnfigurationsobjekt.
     */
    private void initAkka(Config config) {
        system = ActorSystem.create(configApp.getString("names.akka.system"), config);
        log = Logging.getLogger(system, this);
        inbox = Inbox.create(system);
    }

    /**
     * Initialisiert den Reader.
     */
    private void initReader() {
        reader = system.actorOf(Props.create(Reader.class), configApp.getString("names.reader"));
    }

    /**
     * Initialisiert die Worker.
     */
    private void initWorkers() {
        system.actorOf(FromConfig.getInstance().props(Props.create(RecordModifier.class)),
                       configApp.getString("names.recordModifier"));
        system.actorOf(FromConfig.getInstance().props(Props.create(CSV2Record.class)),
                       configApp.getString("names.csv2Record"));
    }

    /**
     * Initialisiert den Writer.
     */
    private void initWriter() throws AkkaBatchException {
        /* der Writer */
        ActorRef writer = system.actorOf(Props.create(Writer.class), configApp.getString("names.writer"));
        inbox.send(writer, new InitWriter(outfileName, configApp.getString("charset.outfile")));
        log.info("warten auf Writer-Intialisierung...");
        Object msg = inbox.receive(Duration.create(5, TimeUnit.SECONDS));
        if (msg instanceof InitResult) {
            InitResult initResult = (InitResult) msg;
            if (!initResult.getSuccess()) {
                throw new AkkaBatchException("Fehler bei der Initialisierung des Writer");
            }
        } else {
            throw new AkkaBatchException("unbekannte Antwort");
        }
        log.info("Writer-Intialisierung fertig.");
    }

    /**
     * wartet dass die Verarbeitung abgeschlossen ist
     *
     * @throws AkkaBatchException
     */
    private void waitForWorkDone() throws AkkaBatchException {
        long startTime = System.currentTimeMillis();
        // auf WorkDone warten
        Object msg =
                inbox.receive(Duration.create(configApp.getLong("times.maxRunDuration"), TimeUnit.SECONDS));
        long endTime = System.currentTimeMillis();
        if (msg instanceof WorkDone) {
            System.out.println(
                    MessageFormat.format("Verarbeitung {0}, Dauer: {1} ms",
                                         ((WorkDone) msg).getSuccess() ? "OK" : "Fehler",
                                         endTime - startTime)
            );
        } else {
            throw new AkkaBatchException(
                    MessageFormat.format("unerwartete Nachricht: {0}", msg.getClass().getCanonicalName()));
        }
    }
}
