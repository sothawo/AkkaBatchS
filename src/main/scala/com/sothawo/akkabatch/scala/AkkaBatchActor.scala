package com.sothawo.akkabatch.scala

import akka.actor.{ActorLogging, Actor}
import akka.event.Logging
import com.typesafe.config.Config

/**
 * base class for the actors. provides a log field via the ActorLogging trait and configuration.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
abstract class AkkaBatchActor extends Actor with ActorLogging {

  /** global configuration object */
  protected var globalConfig: Config = _

  /** application configuration */
  protected var appConfig: Config = _

  override def preStart() {
    super.preStart()
    globalConfig = context.system.settings.config
    appConfig = globalConfig getConfig("com.sothawo.akkabatch")
  }
}
