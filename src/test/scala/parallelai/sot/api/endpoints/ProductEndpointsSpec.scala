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
import parallelai.common.secure.diffiehellman.{ClientPublicKey, DiffieHellmanServer, ServerPublicKey}
import parallelai.common.secure.{AES, CryptoMechanic, Encrypted}
import parallelai.sot.api.config.{secret, _}
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.model.{Product, ProductToken}

class ProductEndpointsSpec extends WordSpec with MustMatchers with ResponseOps {
  val licenceErrorMessage = "Mocked Licence Error Message"

  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  val licenceHostExpectation: Request[_, _] => Boolean =
    _.uri.host.contains(licence.name)

  val registerProductPathExpectation: Request[_, _] => Boolean =
    _.uri.path.startsWith(Seq(licence.context, licence.version, "product", "register"))

  "Licence endpoints" should {
    "fail to register product when product token sanity check is invalid" in new ProductEndpoints {
      override protected val createClientPublicKey: ClientPublicKey = ClientPublicKey("client-public-key".getBytes)

      val bodyExpectation: Request[_, _] => Boolean =
        _.body.asInstanceOf[StringBody].s.parseJson.containsFields("code", "email", "token")

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespondWithCode(Status.Unauthorized.code, Response(Error(licenceErrorMessage), Status.Unauthorized))
      }

      lazy val registerProduct: Endpoint[Response] = super.registerProduct

      val productToken = ProductToken("licenceId", "productCode", "productEmail")
      val product = Product("wrongCode", "wrongEmail", Encrypted(productToken))

      val Some(response) = registerProduct(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

      response.status mustEqual Status.Unauthorized
      response.content.convertTo[Error].message mustEqual licenceErrorMessage
    }

    "fail to register product when product token sanity check is invalid and no client public key was generated" in new ProductEndpoints {
      val bodyExpectation: Request[_, _] => Boolean =
        _.body.asInstanceOf[StringBody].s.parseJson.containsFields("code", "email", "token")

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespondWithCode(Status.Unauthorized.code, Response(Errors(licenceErrorMessage, licenceErrorMessage), Status.Unauthorized))
      }

      lazy val registerProduct: Endpoint[Response] = super.registerProduct

      val productToken = ProductToken("licenceId", "productCode", "productEmail")
      val product = Product("wrongCode", "wrongEmail", Encrypted(productToken))

      val Some(response) = registerProduct(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

      response.status mustEqual Status.Unauthorized
      response.content.convertTo[Errors].messages mustEqual Seq(licenceErrorMessage, licenceErrorMessage)
    }

    "register product" in new ProductEndpoints {
      val bodyExpectation: Request[_, _] => Boolean =
        _.body.asInstanceOf[StringBody].s.parseJson.containsFields("code", "email", "token")

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Response(DiffieHellmanServer.create(createClientPublicKey)._1).toJson.prettyPrint)
      }

      lazy val registerProduct: Endpoint[Response] = super.registerProduct

      val productToken = ProductToken("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Encrypted(productToken))

      val Some(response) = registerProduct(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      println(response.content.convertTo[ServerPublicKey]) // TODO Actual assertion
    }
  }
}