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
 * Applikationclass.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
public class BatchApp {
// ------------------------------ FIELDS ------------------------------

    /** Logger */
    protected LoggingAdapter log;

    /** name of the configuration file */
    private final String configFileName;

    /** name of the input file */
    private String infileName = "not_specified";

    /** name of the output file */
    private String outfileName = "not_specified";

    /** application configuration */
    private Config configApp;

    /** the actor system */
    private ActorSystem system;

    /** the Inbox object of the actor system */
    private Inbox inbox;

    /** the Reader */
    private ActorRef reader;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * @param args
     *         program arguments
     */
    public BatchApp(String[] args) {
        if (null == args || args.length < 1) {
            throw new IllegalArgumentException("wrong call; arguments: <config> [<infile> <outfile>]");
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
     *         program arguments
     */
    public static void main(String[] args) {
        new BatchApp(args).run();
    }

    /**
     * Main execution method of BatchApp.
     */
    private void run() {
        try {
            System.out.println("#Cores: " + Runtime.getRuntime().availableProcessors());
            Config configAll = getConfig();

            initAkka(configAll);

            RecordProcessor.fibonacci = configApp.getInt("simulation.fibonacci");
            RecordProcessor.threadsleep = configApp.getInt("simulation.threadsleep");
            log.info(MessageFormat.format("Simulation: Fibonacci {0}, ThreadSleep {1}", RecordProcessor.fibonacci,
                                          RecordProcessor.threadsleep));

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
                log.debug("starting processing...");
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
     * reads the configuration from the file system
     *
     * @return configuration
     */
    private Config getConfig() {

        // config from the file system
        Config configFile = ConfigFactory.parseFile(new File(configFileName));

        // application configuration from the classpath without resolving, this is done after the merge
        ConfigParseOptions parseOptions = ConfigParseOptions.defaults();
        ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true);
        Config configAll = ConfigFactory.load("application", parseOptions, resolveOptions);

        // merge and then resolve
        configAll = configFile.withFallback(configAll).resolve();

        configApp = configAll.getConfig("com.sothawo.akkabatch");
        return configAll;
    }

    /**
     * initializes the akka system and the logger
     *
     * @param config
     *         configuration object
     */
    private void initAkka(Config config) {
        system = ActorSystem.create(configApp.getString("names.akka.system"), config);
        log = Logging.getLogger(system, this);
        inbox = Inbox.create(system);
    }

    /**
     * initializes the Reader
     */
    private void initReader() {
        reader = system.actorOf(Props.create(Reader.class), configApp.getString("names.reader"));
    }

    /**
     * Initiaslizes the Writer
     */
    private void initWriter() throws AkkaBatchException {
        /* the Writer */
        ActorRef writer = system.actorOf(Props.create(Writer.class), configApp.getString("names.writer"));
        inbox.send(writer, new InitWriter(outfileName, configApp.getString("charset.outfile")));

        log.info("waiting for Writer to be initialized...");
        Object msg = inbox.receive(Duration.create(5, TimeUnit.SECONDS));

        if (msg instanceof InitResult) {
            InitResult initResult = (InitResult) msg;
            if (!initResult.isSuccess()) {
                throw new AkkaBatchException("Error initializing the Writer");
            }
        } else {
            throw new AkkaBatchException("unknown answer");
        }
        log.info("Writer-Initialization ready.");
    }

    /**
     * Intializes the Worke actors
     */
    private void initWorkers() {
        system.actorOf(FromConfig.getInstance().props(Props.create(RecordModifier.class)),
                       configApp.getString("names.recordModifier"));
        system.actorOf(FromConfig.getInstance().props(Props.create(CSV2Record.class)),
                       configApp.getString("names.csv2Record"));
    }

    /**
     * waits for the processing to be done
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
            System.out.println(MessageFormat.format("result {0}, Elapsed time: {1} ms",
                                         ((WorkDone) msg).isSuccess() ? "OK" : "Error",
                                         endTime - startTime)
            );
        } else {
            throw new AkkaBatchException(
                    MessageFormat.format("unexpected message: {0}", msg.getClass().getCanonicalName()));
        }
    }
}
