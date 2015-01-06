package play.it.http

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.mockito._

import play.api.test._
import play.api.mvc._

import play.core.j.JavaHelpers
import play.mvc.Http
import play.mvc.Http.{ RequestBody, Context }

/**
 *
 */
class JavaRequestsSpec extends PlaySpecification with Mockito {

  "JavaHelpers" should {

    "create a request with a helper that can do cookies" in {
      import scala.collection.JavaConversions._

      val cookie1 = Cookie("name1", "value1")
      val requestHeader: RequestHeader = FakeRequest().withCookies(cookie1)
      val javaRequest: Http.Request = JavaHelpers.createJavaRequest(requestHeader)

      val iterator: Iterator[Http.Cookie] = javaRequest.cookies().iterator()
      val cookieList = iterator.toList

      cookieList.size must be equalTo 1
      cookieList(0).name must be equalTo "name1"
      cookieList(0).value must be equalTo "value1"
    }

    "create a context with a helper that can do cookies" in {
      import scala.collection.JavaConversions._

      val cookie1 = Cookie("name1", "value1")

      val requestHeader: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest().withCookies(cookie1), new RequestBody())
      val javaContext: Context = JavaHelpers.createJavaContext(requestHeader)
      val javaRequest = javaContext.request()

      val iterator: Iterator[Http.Cookie] = javaRequest.cookies().iterator()
      val cookieList = iterator.toList

      cookieList.size must be equalTo 1
      cookieList(0).name must be equalTo "name1"
      cookieList(0).value must be equalTo "value1"
    }

  }

}
