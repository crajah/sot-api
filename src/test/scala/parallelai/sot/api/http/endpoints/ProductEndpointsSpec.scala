package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import io.finch.Application
import io.finch.Input._
import io.finch.circe._
import javax.crypto.SecretKey
import spray.json._
import org.scalatest.{MustMatchers, WordSpec}
import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{Request, StringBody}
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, DiffieHellmanServer}
import parallelai.common.secure.{AES, Crypto, CryptoMechanic, Encrypted}
import parallelai.sot.api.config.{secret, _}
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.model.{ApiSharedSecret, IdGenerator99UniqueSuffix, Product, ProductToken, RegisteredProduct}

class ProductEndpointsSpec extends WordSpec with MustMatchers with IdGenerator99UniqueSuffix {
  val licenceErrorMessage = "Mocked Licence Error Message"

  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  val licenceHostExpectation: Request[_, _] => Boolean =
    _.uri.host.contains(licence.name)

  val registerProductPathExpectation: Request[_, _] => Boolean =
    _.uri.path.startsWith(Seq(licence.context, licence.version, "product", "register"))

  val bodyExpectation: Request[_, _] => Boolean =
    _.body.asInstanceOf[StringBody].s.parseJson.containsFields("code", "email", "token")

  "Licence endpoints" should {
    "fail to register product when product token sanity check is invalid" in {
      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Result(Errors(licenceErrorMessage), Status.Unauthorized))

      new ProductEndpoints {
        val productToken = ProductToken("licenceId", "productCode", "productEmail")
        val product = Product("wrongCode", "wrongEmail", Encrypted(productToken))

        val Some(result) = productEndpoints(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

        result.status mustEqual Status.Unauthorized
        result.value mustBe Left(Errors(licenceErrorMessage))
      }
    }

    "fail to register product when product token sanity check is invalid and no client public key was generated" in {
      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Result(Errors(licenceErrorMessage, licenceErrorMessage), Status.Unauthorized))

      new ProductEndpoints {
        val productToken = ProductToken("licenceId", "productCode", "productEmail")
        val product = Product("wrongCode", "wrongEmail", Encrypted(productToken))

        val Some(result) = productEndpoints(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

        result.status mustEqual Status.Unauthorized
        result.value mustBe Left(Errors(licenceErrorMessage, licenceErrorMessage))
      }
    }

    "register product" in {
      val clientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey

      val (serverPublicKey, serverSharedSecret) = DiffieHellmanServer create clientPublicKey

      val aesSecretKey: SecretKey = Crypto.aesSecretKey

      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Result(RegisteredProduct(serverPublicKey, Encrypted(ApiSharedSecret(uniqueId(), aesSecretKey))), Status.Ok))

      new ProductEndpoints {
        val productToken = ProductToken("licenceId", "productCode", "productEmail")
        val product = Product(productToken.code, productToken.email, Encrypted(productToken))

        val Some(result) = productEndpoints(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

        result.status mustEqual Status.Ok

        val Right(registeredProduct) = result.value

        registeredProduct.serverPublicKey mustEqual serverPublicKey

        registeredProduct.apiSharedSecret.decrypt must have(
          'licenceId (uniqueId()),
          'apiServerSecret (aesSecretKey)
        )

        registerProduct.licences(uniqueId()) mustBe a [ClientSharedSecret]
      }
    }
  }
}