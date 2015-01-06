/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
import play.sbtplugin.run.PlayWatchService
import sbt._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.Properties

object DevModeBuild {

  def jdk7WatchService = Def.setting {
    if (Properties.isJavaAtLeast("1.7")) {
      PlayWatchService.jdk7(Keys.sLog.value)
    } else {
      println("Not testing JDK7 watch service because we're not on JDK7")
      PlayWatchService.sbt(Keys.pollInterval.value)
    }
  }

  def jnotifyWatchService = Def.setting {
    PlayWatchService.jnotify(Keys.target.value)
  }

  val MaxAttempts = 10
  val WaitTime = 500l

  @tailrec
  def verifyResourceContains(path: String, status: Int, assertions: Seq[String], attempts: Int): Unit = {
    println(s"Attempt $attempts at $path")
    val messages = ListBuffer.empty[String]
    try {
      val url = new java.net.URL("http://localhost:9000" + path)
      val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]

      if (status == conn.getResponseCode) {
        messages += s"Resource at $path returned $status as expected"
      } else {
        throw new RuntimeException(s"Resource at $path returned ${conn.getResponseCode} instead of $status")
      }

      val is = if (conn.getResponseCode >= 400) {
        conn.getErrorStream
      } else {
        conn.getInputStream
      }

      // The input stream may be null if there's no body
      val contents = if (is != null) {
        val c = IO.readStream(is)
        is.close()
        c
      } else ""
      conn.disconnect()

      assertions.foreach { assertion =>
        if (contents.contains(assertion)) {
          messages += s"Resource at $path contained $assertion"
        } else {
          throw new RuntimeException(s"Resource at $path didn't contain '$assertion':\n$contents")
        }
      }

      messages.foreach(println)
    } catch {
      case e: Exception =>
        if (attempts < MaxAttempts) {
          Thread.sleep(WaitTime)
          verifyResourceContains(path, status, assertions, attempts + 1)
        } else {
          messages.foreach(println)
          println(s"After $attempts attempts:")
          throw e
        }
    }
  }
}