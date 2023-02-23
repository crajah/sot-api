package parallelai.sot.api.http

import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }
import com.softwaremill.sttp._
import com.softwaremill.sttp.testing._

/**
 * Note - instead of using Future when making real HTTP calls it would be better to use e.g.
 * async-http-client-backend-monix, wrapping responses in monix.eval.Task
 */
class SttpSpec extends WordSpec with MustMatchers with ScalaFutures {
  def serverHealth[F[_], S](implicit ev: SttpBackend[F, S]): F[Response[String]] = {
    val request: Request[String, Nothing] = sttp.get(uri"http://my-server/health")
    request.send()
  }

  "Sttp" should {
    "send request synchronously" in {
      implicit val backend: SttpBackendStub[Id, Nothing] = SttpBackendStub.synchronous
        .whenRequestMatches(_.uri.path.startsWith(Seq("health")))
        .thenRespond("Synchronous")

      val response: Id[Response[String]] = serverHealth[Id, Nothing]

      response.unsafeBody mustEqual "Synchronous"
    }

    "send request asynchronously via Future" in {
      implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(_.uri.path.startsWith(Seq("health")))
        .thenRespond("Asynchronous")

      whenReady(serverHealth[Future, Nothing]) { response =>
        response.unsafeBody mustEqual "Asynchronous"
      }
    }
  }
}