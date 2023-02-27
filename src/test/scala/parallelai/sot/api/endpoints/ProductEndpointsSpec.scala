package parallelai.sot.api.endpoints

import scala.collection.mutable.ArrayOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success
import io.finch.{ Application, Endpoint }
import io.finch.Input._
import spray.json._
import spray.json.lenses.JsonLenses._
import org.scalatest.{ MustMatchers, WordSpec }
import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{ Response => _ }
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.Response
import parallelai.sot.api.config._
import parallelai.sot.api.entities.{ Organisation, ProductRegister }
import io.finch.sprayjson._
import parallelai.common.secure.CryptoMechanic
import parallelai.common.secure.model.EncryptedBytes
import parallelai.sot.api.config.secret
import org.scalatest.TryValues._

class ProductEndpointsSpec extends WordSpec with MustMatchers {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = secret.getBytes)

  "Licence endpoints" should {
    "register product" in new ProductEndpoints {
      implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture

      val registerProduct: Endpoint[Response] = super.registerProduct

      val organisation = Organisation("org-id", "org-code", "org@gmail.com")
      val encryptedProductToken = EncryptedBytes(organisation)

      val productRegister = ProductRegister(organisation, encryptedProductToken)

      val Some(response) = registerProduct(post(p"/$productPath/register").withBody[Application.Json](productRegister)).awaitValueUnsafe()

      /*implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(req => req.uri.host.contains(licence.name) && req.uri.path.startsWith(Seq(licence.context, "2", "health")))
        .thenRespond(Response(s"Successfully pinged service ${licence.name}").toJson.prettyPrint)

      val licenceHealth: Endpoint[Response] = super.licenceHealth

      val Some(response) = licenceHealth(get(p"/$healthPath/${licence.context}")).awaitValueUnsafe()*/

      response.status mustEqual Status.Ok

      response.content.convertTo[ProductRegister] must have(
        'organisation(organisation))
    }
  }
}