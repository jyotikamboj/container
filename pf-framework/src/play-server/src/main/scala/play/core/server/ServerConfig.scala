/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import com.typesafe.config.{ Config, ConfigFactory }
import java.io.File
import java.util.Properties
import play.api.{ Configuration, Mode }

/**
 * Common configuration for servers such as NettyServer.
 *
 * @param rootDir The root directory of the server. Used to find default locations of
 * files, log directories, etc.
 * @param port The HTTP port to use.
 * @param sslPort The HTTPS port to use.
 * @param address The socket address to bind to.
 * @param mode The run mode: dev, test or prod.
 * @param configuration: The configuration to use for loading the server. This is not
 * the same as application configuration. This configuration is usually loaded from a
 * server.conf file, whereas the application configuration is usually loaded from an
 * application.conf file.
 */
case class ServerConfig(
    rootDir: File,
    port: Option[Int],
    sslPort: Option[Int],
    address: String,
    mode: Mode.Mode,
    properties: Properties,
    configuration: Configuration) {
  // Some basic validation of config
  if (!port.isDefined && !sslPort.isDefined) throw new IllegalArgumentException("Must provide either an HTTP port or an HTTPS port")
}

object ServerConfig {

  def apply(
    rootDir: File,
    port: Option[Int],
    sslPort: Option[Int] = None,
    address: String = "0.0.0.0",
    mode: Mode.Mode = Mode.Prod,
    properties: Properties): ServerConfig = {
    ServerConfig(
      rootDir = rootDir,
      port = port,
      sslPort = sslPort,
      address = address,
      mode = mode,
      properties = properties,
      configuration = loadConfiguration(properties, rootDir)
    )
  }

  /**
   * Create a server Configuration object (a wrapper around a Typesafe Config object)
   * given some Properties. At this moment this just reads from server-reference.conf
   * and from the given properties.
   *
   * @param properties The properties to base the configuration on.
   */
  def loadConfiguration(properties: Properties): Configuration = {
    Configuration(loadDefaultConfig(properties).resolve())
  }

  /**
   * Creates a server Configuration with `loadConfiguration(Properties)` but also
   * sets the given rootDir property as a low-priority configuration option
   * with the key "play.server.dir".
   */
  def loadConfiguration(properties: Properties, rootDir: File): Configuration = {
    val javaMap = new java.util.HashMap[String, String]()
    javaMap.put("play.server.dir", rootDir.getAbsolutePath)
    val rootDirConfig = ConfigFactory.parseMap(javaMap)
    val config = loadDefaultConfig(properties).withFallback(rootDirConfig).resolve()
    Configuration(config)
  }

  private def loadDefaultConfig(properties: Properties): Config = {
    // TODO: Flesh the logic out here so it is closer to the standard ConfigFactory.load()
    // logic. E.g. support reading from server.conf resource, support overriding config
    // file location with properties, etc.
    val serverReferenceConfig = ConfigFactory.parseResources("server-reference.conf")
    val systemPropertyConfig = ConfigFactory.parseProperties(properties)
    systemPropertyConfig.withFallback(serverReferenceConfig)
  }

}