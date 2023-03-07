package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import io.finch.Endpoint
import io.finch.Input._
import spray.json._
import spray.json.lenses.JsonLenses._
import org.scalatest.{MustMatchers, WordSpec}
import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{Response => _}
import com.twitter.finagle.http.Status
import parallelai.sot.api.config._

class HealthEndpointsSpec extends WordSpec with MustMatchers {
  "Health endpoints" should {
    "indicate a healthy service" in new HealthEndpoints {
      val Some(response) = health(get(p"/$healthPath")).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      response.content.convertTo[String] mustEqual s"Successfully pinged service ${api.name}"
    }

    "indicate a healthy licence service" in new HealthEndpoints {
      implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(req => req.uri.host.contains(licence.name) && req.uri.path.startsWith(Seq(licence.context, licence.version, "health")))
        .thenRespond(Response(s"Successfully pinged service ${licence.name}").toJson.prettyPrint)

      val licenceHealth: Endpoint[Response] = super.licenceHealth

      val Some(response) = licenceHealth(get(p"/$healthPath/${licence.context}")).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      response.content.extract[String]("content") mustEqual s"Successfully pinged service ${licence.name}"
    }
  }
}