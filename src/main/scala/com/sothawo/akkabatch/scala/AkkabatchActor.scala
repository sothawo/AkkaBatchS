package com.sothawo.akkabatch.scala

import akka.actor.Actor
import akka.event.Logging
import com.typesafe.config.Config

/**
 * base class for the actors. provides logger and configuration.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
abstract class AkkabatchActor extends Actor {

  /** Logger */
  protected val log = Logging(context.system, this)

  /** global configuration object */
  protected var globalConfig: Config = _

  /** application configuration */
  protected var appConfig: Config = _

  override def preStart(): Unit = {
    super.preStart()
    globalConfig = context.system.settings.config
    appConfig = globalConfig.getConfig("com.sothawo.akkabatch")
  }
}
