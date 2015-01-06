/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.ws.ning

import org.specs2.mock.Mockito

import com.ning.http.client.{ Response => AHCResponse, RequestBuilder, FluentCaseInsensitiveStringsMap, AsyncHttpClient }
import com.ning.http.client.cookie.{ Cookie => AHCCookie }

import play.api.mvc._

import java.util
import play.api.libs.ws._
import play.api.test._

object NingWSSpec extends PlaySpecification with Mockito {

  sequential

  "Ning WS" should {

    object PairMagnet {
      implicit def fromPair(pair: Pair[WSClient, java.net.URL]) =
        new WSRequestHolderMagnet {
          def apply(): WSRequestHolder = {
            val (client, netUrl) = pair
            client.url(netUrl.toString)
          }
        }
    }

    "support ning magnet" in new WithApplication {
      import scala.language.implicitConversions
      import PairMagnet._

      val client = WS.client
      val exampleURL = new java.net.URL("http://example.com")
      WS.url(client -> exampleURL) must beAnInstanceOf[WSRequestHolder]
    }

    "support direct client instantiation" in new WithApplication {
      val sslBuilder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
      implicit val sslClient = new play.api.libs.ws.ning.NingWSClient(sslBuilder.build())
      WS.clientUrl("http://example.com/feed") must beAnInstanceOf[WSRequestHolder]
    }

    "NingWSClient.underlying" in new WithApplication {
      val client = WS.client
      client.underlying[AsyncHttpClient] must beAnInstanceOf[AsyncHttpClient]
    }

    "NingWSCookie.underlying" in new WithApplication() {
      import com.ning.http.client.cookie.Cookie

      val mockCookie = mock[Cookie]
      val cookie = new NingWSCookie(mockCookie)
      val thisCookie = cookie.underlying[Cookie]
    }

    "NingWSRequest.setHeaders using a builder with fluent map" in new WithApplication {
      val request = new NingWSRequest(mock[NingWSClient], "GET", None, None, Map.empty, EmptyBody, new RequestBuilder("GET"))
      val headerMap: java.util.Map[String, java.util.Collection[String]] = new java.util.HashMap()
      headerMap.put("key", java.util.Arrays.asList("value"))

      val ningRequest = request.setHeaders(new FluentCaseInsensitiveStringsMap(headerMap)).build
      ningRequest.getHeaders.containsKey("key") must beTrue
    }

    "NingWSRequest.setHeaders using a builder with direct map" in new WithApplication {
      val request = new NingWSRequest(mock[NingWSClient], "GET", None, None, Map.empty, EmptyBody, new RequestBuilder("GET"))
      val headerMap: Map[String, Seq[String]] = Map("key" -> Seq("value"))
      val ningRequest = request.setHeaders(headerMap).build
      ningRequest.getHeaders.containsKey("key") must beTrue
    }

    "NingWSRequest.setQueryString" in new WithApplication {
      val request = new NingWSRequest(mock[NingWSClient], "GET", None, None, Map.empty, EmptyBody, new RequestBuilder("GET"))
      val queryString: Map[String, Seq[String]] = Map("key" -> Seq("value"))
      val ningRequest = request.setQueryString(queryString).build
      ningRequest.getQueryParams().containsKey("key") must beTrue
    }

    "support several query string values for a parameter" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withQueryString("foo" -> "foo1", "foo" -> "foo2").asInstanceOf[NingWSRequestHolder]
        .prepare().build
      req.getQueryParams.get("foo").contains("foo1") must beTrue
      req.getQueryParams.get("foo").contains("foo2") must beTrue
      req.getQueryParams.get("foo").size must equalTo(2)
    }

    "support http headers" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withHeaders("key" -> "value1", "key" -> "value2").asInstanceOf[NingWSRequestHolder]
        .prepare().build
      req.getHeaders.get("key").contains("value1") must beTrue
      req.getHeaders.get("key").contains("value2") must beTrue
      req.getHeaders.get("key").size must equalTo(2)
    }

    "not make Content-Type header if there is Content-Type in headers already" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withHeaders("Content-Type" -> "text/plain").withBody(<aaa>value1</aaa>).asInstanceOf[NingWSRequestHolder]
        .prepare().build
      req.getHeaders.get("Content-Type").contains("text/plain") must beTrue
      req.getHeaders.get("Content-Type").size must equalTo(1)
    }

    "support a virtual host" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withVirtualHost("192.168.1.1").asInstanceOf[NingWSRequestHolder]
        .prepare().build
      req.getVirtualHost must be equalTo "192.168.1.1"
    }

    "support follow redirects" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withFollowRedirects(true).asInstanceOf[NingWSRequestHolder]
        .prepare().build
      req.isRedirectEnabled must beTrue
    }

    "support timeout" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withRequestTimeout(1000).asInstanceOf[NingWSRequestHolder]
        .prepare().build
      req.getPerRequestConfig.getRequestTimeoutInMs must be equalTo 1000
    }

    "not support invalid timeout" in new WithApplication {
      WS.url("http://playframework.com/").withRequestTimeout(-1) should throwAn[IllegalArgumentException]
    }

    "support a proxy server" in new WithApplication {
      val proxy = DefaultWSProxyServer(protocol = Some("https"), host = "localhost", port = 8080, principal = Some("principal"), password = Some("password"))
      val req = WS.url("http://playframework.com/").withProxyServer(proxy).asInstanceOf[NingWSRequestHolder].prepare().build
      val actual = req.getProxyServer

      actual.getProtocolAsString must be equalTo "https"
      actual.getHost must be equalTo "localhost"
      actual.getPort must be equalTo 8080
      actual.getPrincipal must be equalTo "principal"
      actual.getPassword must be equalTo "password"
    }

    "support a proxy server" in new WithApplication {
      val proxy = DefaultWSProxyServer(host = "localhost", port = 8080)
      val req = WS.url("http://playframework.com/").withProxyServer(proxy).asInstanceOf[NingWSRequestHolder].prepare().build
      val actual = req.getProxyServer

      actual.getProtocolAsString must be equalTo "http"
      actual.getHost must be equalTo "localhost"
      actual.getPort must be equalTo 8080
      actual.getPrincipal must beNull
      actual.getPassword must beNull
    }

    val patchFakeApp = FakeApplication(withRoutes = {
      case ("PATCH", "/") => Action {
        Results.Ok(play.api.libs.json.Json.parse(
          """{
            |  "data": "body"
            |}
          """.stripMargin))
      }
    })

    "support patch method" in new WithServer(patchFakeApp) {
      // NOTE: if you are using a client proxy like Privoxy or Polipo, your proxy may not support PATCH & return 400.
      val req = WS.url("http://localhost:" + port + "/").patch("body")

      val rep = await(req)

      rep.status must ===(200)
      (rep.json \ "data").asOpt[String] must beSome("body")
    }

    def gzipFakeApp = {
      import java.io._
      import java.util.zip._
      FakeApplication(
        withRoutes = {
          case ("GET", "/") => Action { request =>
            request.headers.get("Accept-Encoding") match {
              case Some(encoding) if encoding.contains("gzip") =>
                val os = new ByteArrayOutputStream
                val gzipOs = new GZIPOutputStream(os)
                gzipOs.write("gziped response".getBytes("utf-8"))
                gzipOs.close()
                Results.Ok(os.toByteArray).as("text/plain").withHeaders("Content-Encoding" -> "gzip")
              case _ =>
                Results.Ok("plain response")
            }
          }
        },
        additionalConfiguration = Map("ws.compressionEnabled" -> true)
      )
    }

    "support gziped encoding" in new WithServer(gzipFakeApp) {

      val req = WS.url("http://localhost:" + port + "/").get()
      val rep = await(req)
      rep.body must ===("gziped response")
    }
  }

  "Ning WS Response" should {
    "get cookies from an AHC response" in {

      val ahcResponse: AHCResponse = mock[AHCResponse]
      val (name, value, domain, path, expires, maxAge, secure, httpOnly) =
        ("someName", "someValue", "example.com", "/", 2000L, 1000, false, false)

      val ahcCookie: AHCCookie = new AHCCookie(name, value, value, domain, path, expires, maxAge, secure, httpOnly)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = NingWSResponse(ahcResponse)

      val cookies: Seq[WSCookie] = response.cookies
      val cookie = cookies(0)

      cookie.domain must ===(domain)
      cookie.name must beSome(name)
      cookie.value must beSome(value)
      cookie.path must ===(path)
      cookie.expires must beSome(expires)
      cookie.maxAge must beSome(maxAge)
      cookie.secure must beFalse
    }

    "get a single cookie from an AHC response" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val (name, value, domain, path, expires, maxAge, secure, httpOnly) =
        ("someName", "someValue", "example.com", "/", 2000L, 1000, false, false)

      val ahcCookie: AHCCookie = new AHCCookie(name, value, value, domain, path, expires, maxAge, secure, httpOnly)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = NingWSResponse(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[WSCookie].which {
        cookie =>
          cookie.name must beSome(name)
          cookie.value must beSome(value)
          cookie.domain must ===(domain)
          cookie.path must ===(path)
          cookie.expires must beSome(expires)
          cookie.maxAge must beSome(maxAge)
          cookie.secure must beFalse
      }
    }

    "return -1 values of expires and maxAge as None" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]

      val ahcCookie: AHCCookie = new AHCCookie("someName", "value", "value", "domain", "path", -1L, -1, false, false)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = NingWSResponse(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[WSCookie].which { cookie =>
        cookie.expires must beNone
        cookie.maxAge must beNone
      }
    }

    "get the body as bytes from the AHC response" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val bytes = Array[Byte](-87, -72, 96, -63, -32, 46, -117, -40, -128, -7, 61, 109, 80, 45, 44, 30)
      ahcResponse.getResponseBodyAsBytes returns bytes
      val response = NingWSResponse(ahcResponse)
      response.bodyAsBytes must_== bytes
    }

    "get headers from an AHC response in a case insensitive map" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val ahcHeaders = new FluentCaseInsensitiveStringsMap()
      ahcHeaders.add("Foo", "bar")
      ahcHeaders.add("Foo", "baz")
      ahcHeaders.add("Bar", "baz")
      ahcResponse.getHeaders returns ahcHeaders
      val response = NingWSResponse(ahcResponse)
      val headers = response.allHeaders
      headers must beEqualTo(Map("Foo" -> Seq("bar", "baz"), "Bar" -> Seq("baz")))
      headers.contains("foo") must beTrue
      headers.contains("Foo") must beTrue
      headers.contains("BAR") must beTrue
      headers.contains("Bar") must beTrue
    }
  }

  "Ning WS Config" should {
    "support overriding secure default values" in {
      val ahcConfig = new NingAsyncHttpClientConfigBuilder().modifyUnderlying { builder =>
        builder.setCompressionEnabled(true)
        builder.setFollowRedirects(false)
      }.build()
      ahcConfig.isCompressionEnabled must beTrue
      ahcConfig.isRedirectEnabled must beFalse
      ahcConfig.getConnectionTimeoutInMs must_== Defaults.connectionTimeout
      ahcConfig.getRequestTimeoutInMs must_== Defaults.requestTimeout
      ahcConfig.getIdleConnectionTimeoutInMs must_== Defaults.idleTimeout
    }
  }

}
