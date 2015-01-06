package play.it

import org.specs2.execute._
import play.api.{ Application, FakeApplication }
import play.core.server.NettyServer
import play.core.server.ServerProvider
import play.core.server.akkahttp.AkkaHttpServer
import AsResult._

/**
 * Helper for creating tests that test integration with different server
 * backends. Common integration tests should implement this trait, then
 * two specific tests should be created, one extending NettyIntegrationSpecification
 * and another extending AkkaHttpIntegrationSpecification.
 *
 * When a test extends this trait it will automatically get overridden versions of
 * TestServer and WithServer that delegate to the correct server backend.
 */
trait ServerIntegrationSpecification extends PendingUntilFixed {
  parent =>
  def integrationServerProvider: ServerProvider

  implicit class UntilAkkaHttpFixed[T: AsResult](t: => T) {
    /**
     * Don't complain if a test fails, but given an error if it passes so we know to
     * remove the pending tag.
     */
    def pendingUntilAkkaHttpFixed: Result = parent match {
      case _: NettyIntegrationSpecification => ResultExecution.execute(AsResult(t))
      case _: AkkaHttpIntegrationSpecification => new PendingUntilFixed(t).pendingUntilFixed
    }
    /**
     * We may want to skip some tests if they're slow due to timeouts. This tag
     * won't remind us if the tests start passing.
     */
    def skipUntilAkkaHttpFixed: Result = parent match {
      case _: NettyIntegrationSpecification => ResultExecution.execute(AsResult(t))
      case _: AkkaHttpIntegrationSpecification => Skipped()
    }
  }

  /**
   * Override the standard TestServer factory method.
   */
  def TestServer(
    port: Int,
    application: Application = play.api.FakeApplication(),
    sslPort: Option[Int] = None): play.api.test.TestServer = {
    play.api.test.TestServer(port, application, sslPort, integrationServerProvider)
  }

  /**
   * Override the standard WithServer class.
   */
  abstract class WithServer(
    app: play.api.test.FakeApplication = play.api.test.FakeApplication(),
    port: Int = play.api.test.Helpers.testServerPort) extends play.api.test.WithServer(app, port, serverProvider = integrationServerProvider)

}
trait NettyIntegrationSpecification extends ServerIntegrationSpecification {
  override def integrationServerProvider: ServerProvider = NettyServer.defaultServerProvider
}
trait AkkaHttpIntegrationSpecification extends ServerIntegrationSpecification {
  override def integrationServerProvider: ServerProvider = AkkaHttpServer.defaultServerProvider
}
