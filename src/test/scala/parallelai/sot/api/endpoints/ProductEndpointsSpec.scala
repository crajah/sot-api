package parallelai.sot.api.endpoints

import scala.concurrent.Future
import io.finch.Input._
import io.finch.sprayjson._
import io.finch.{Application, Endpoint}
import spray.json._
import org.scalatest.{MustMatchers, WordSpec}
import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{Request, StringBody}
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{ClientPublicKey, DiffieHellmanClient, DiffieHellmanServer}
import parallelai.common.secure.{CryptoMechanic, Encrypted}
import parallelai.sot.api.config.{secret, _}
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.model.{Product, ProductToken}

class ProductEndpointsSpec extends WordSpec with MustMatchers {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = secret.getBytes)

  "Licence endpoints" should {
    "register product" in new ProductEndpoints {
      override protected val createClientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        def hostExpectation(r: Request[_, _]): Boolean =
          r.uri.host.contains(licence.name)

        def pathExpectation(r: Request[_, _]): Boolean =
          r.uri.path.startsWith(Seq(licence.context, licence.version, "product", "register"))

        def bodyExpectation(r: Request[_, _]): Boolean = {
          val json = r.body.asInstanceOf[StringBody].s.parseJson
          (json / "organisation").isDefined && (json / "productToken").isDefined
        }

        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => hostExpectation(req) && pathExpectation(req) && bodyExpectation(req))
          .thenRespond(Response(DiffieHellmanServer.create(createClientPublicKey)._1).toJson.prettyPrint)
      }

      lazy val registerProduct: Endpoint[Response] = super.registerProduct

      val productToken = ProductToken("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Encrypted(productToken))

      val Some(response) = registerProduct(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

      response.status mustEqual Status.Ok

      // TODO - WIP
      println(response.content.prettyPrint)

      /*response.content.extract[ProductRegister]("content") must matchPattern {
        case ProductRegister(`organisation`, _, _) =>
      }*/
    }
  }
}