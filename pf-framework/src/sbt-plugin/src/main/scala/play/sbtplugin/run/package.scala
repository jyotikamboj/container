/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbtplugin
import sbt._
import play.runsupport.{ LoggerProxy, PlayWatchService => PWS, PlayWatcher => PW }

package object run {
  import scala.language.implicitConversions

  type PlayWatchService = PWS
  type PlayWatcher = PW

  implicit def toLoggerProxy(in: Logger): LoggerProxy = new LoggerProxy {
    def verbose(message: => String): Unit = in.verbose(message)
    def debug(message: => String): Unit = in.debug(message)
    def info(message: => String): Unit = in.info(message)
    def warn(message: => String): Unit = in.warn(message)
    def error(message: => String): Unit = in.error(message)
    def trace(t: => Throwable): Unit = in.trace(t)
    def success(message: => String): Unit = in.success(message)
  }
}
