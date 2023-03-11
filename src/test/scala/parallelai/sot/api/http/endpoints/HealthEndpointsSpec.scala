package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import io.finch.Input._
import org.scalatest.{MustMatchers, WordSpec}
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.sot.api.config._
import parallelai.sot.api.http.Result

class HealthEndpointsSpec extends WordSpec with MustMatchers {
  "Health endpoints" should {
    "indicate a healthy service" in {
      implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

      new HealthEndpoints {
        val Some(result) = health(get(p"/$healthPath")).awaitValueUnsafe()

        result.status mustEqual Status.Ok
        result.value.right.get mustEqual s"Successfully pinged service ${api.name}"
      }
    }

    "indicate a healthy licence service" in {
      implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(req => req.uri.host.contains(licence.name) && req.uri.path.startsWith(Seq(licence.context, licence.version, "health")))
        .thenRespond(Result(s"Successfully pinged service ${licence.name}", Status.Ok))

      new HealthEndpoints {
        val Some(result) = licenceHealth(get(p"/$healthPath/${licence.context}")).awaitValueUnsafe()

        result.status mustEqual Status.Ok
        result.value.right.get mustEqual s"Successfully pinged service ${licence.name}"
      }
    }
  }
}