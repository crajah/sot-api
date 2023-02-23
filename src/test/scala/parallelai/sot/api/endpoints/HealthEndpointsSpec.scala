package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.finch.Endpoint
import io.finch.Input._
import spray.json._
import spray.json.lenses.JsonLenses._
import org.scalatest.{ MustMatchers, WordSpec }
import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{ Response => _ }
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.Response
import parallelai.sot.api.config._

class HealthEndpointsSpec extends WordSpec with MustMatchers {
  "Health endpoints" should {
    "indicate a healthy application" in new HealthEndpoints {
      val Some(response) = health(get(p"/$healthPath")).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      response.content.convertTo[String] mustEqual s"Successfully pinged service ${api.name}"
    }

    "indicate a healthy API Server application" in new HealthEndpoints {
      implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(req => req.uri.host.contains(licence.name) && req.uri.path.startsWith(Seq(licence.name, "2", "health")))
        .thenRespond(Response(s"Successfully pinged service ${licence.name}").toJson.prettyPrint)

      val healthLicence: Endpoint[Response] = super.healthLicence

      val Some(response) = healthLicence(get(p"/$healthPath/${licence.name}")).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      response.content.extract[String]("content") mustEqual s"Successfully pinged service ${licence.name}"
    }
  }
}