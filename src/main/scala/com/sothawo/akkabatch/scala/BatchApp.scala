package com.sothawo.akkabatch.scala

import com.typesafe.config.{ConfigResolveOptions, ConfigParseOptions, ConfigFactory, Config}
import com.sothawo.akkabatch.scala.messages.{WorkDone, InitResult, InitWriter, InitReader}
import java.io.File
import akka.actor.{Props, ActorRef, ActorSystem, Inbox}
import akka.event.{LoggingAdapter, Logging}
import scala.concurrent.duration._
import akka.routing.FromConfig

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
object BatchApp extends App {

  /** Logger */
  var log: LoggingAdapter = _

  /** application configuration */
  private var configApp: Config = null

  /** name of the output file */
  val outfileName = if (args.length > 2) args(2) else "not_specified"

  /** the actor system */
  private var system: ActorSystem = _

  /** the Inbox object of the actor system */
  private var inbox: Inbox = _

  /** the Reader */
  private var reader: ActorRef = _

  try {
    if (null == args || args.length < 1) {
      throw new IllegalArgumentException("wrong call; arguments: <config> [<infile> <outfile>]")
    }

    println(s"#Cores: ${Runtime.getRuntime.availableProcessors}")

    initAkka(getConfig(args(0)))

    RecordProcessor.setFibonacci(configApp.getInt("simulation.fibonacci"))
    RecordProcessor.setThreadsleep(configApp.getInt("simulation.threadsleep"))
    log.info(s"Simulation: Fibonacci ${RecordProcessor.getFibonacci}, ThreadSleep ${RecordProcessor.getThreadsleep}")

    val runMaster = configApp.getBoolean("modules.master")
    val runWorker = configApp.getBoolean("modules.worker")
    log.info(s"master: $runMaster, worker: $runWorker")
    if (runMaster) {
      initReader()
      initWriter()
    }
    if (runWorker) {
      initWorkers()
    }
    if (runMaster) {
      log.debug("starting processing...")
      inbox.send(reader,
        InitReader(if (args.length > 1) args(1) else "not_specified", configApp.getString("charset.infile")))
    }
    waitForWorkDone
  }
  catch {
    case e: Exception => e.printStackTrace
  }
  finally {
    if (null != system) system.shutdown
  }

  /**
   * reads the configuration from the file system
   *
   * @return configuration
   */
  private def getConfig(configFileName: String) = {
    val configFile = ConfigFactory.parseFile(new File(configFileName))
    val parseOptions = ConfigParseOptions.defaults
    val resolveOptions = ConfigResolveOptions.defaults.setAllowUnresolved(true)
    var configAll = ConfigFactory.load("application", parseOptions, resolveOptions)
    configAll = configFile.withFallback(configAll).resolve
    configApp = configAll.getConfig("com.sothawo.akkabatch")
    configAll
  }

  /**
   * initializes the akka system and the logger
   *
   * @param config
     * configuration object
   */
  private def initAkka(config: Config) {
    system = ActorSystem(configApp.getString("names.akka.system"), config)
    log = Logging(system, this.getClass)
    inbox = Inbox.create(system)
  }

  /**
   * initializes the Reader
   */
  def initReader() {
    reader = system.actorOf(Reader.props(), configApp.getString("names.reader"))
  }

  /**
   * Initializes the Writer
   */
  def initWriter() {
    inbox.send(system.actorOf(Writer.props(), configApp.getString("names.writer")),
      InitWriter(outfileName, configApp.getString("charset.outfile")))

    log.info("waiting for Writer to be initialized...")
    inbox.receive(5 seconds) match {
      case msg: InitResult => if (!msg.success) throw new AkkaBatchException("Error initializing the Writer")
      case _ => throw new AkkaBatchException("unknown answer")
    }

    log.info("Writer-Initialization ready.")
  }

  /**
   * Intializes the Worker actors
   */
  def initWorkers() {
    system.actorOf(FromConfig.props(Props[RecordModifier]), configApp.getString("names.recordModifier"))
    system.actorOf(FromConfig.props(Props[CSV2Record]), configApp.getString("names.csv2Record"))
  }

  def waitForWorkDone {
    val startTime = System.currentTimeMillis
    // auf WorkDone warten
    val msg = inbox.receive(configApp.getLong("times.maxRunDuration") seconds)
    val duration = System.currentTimeMillis - startTime

    msg match {
      case WorkDone(success) => log.info(s"success $success, elapsed time: $duration")
      case _ => throw new AkkaBatchException(s"unexpected message: ${msg.getClass.getCanonicalName}")
    }
  }
}
