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
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.config.{secret, _}
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.json.SprayJsonLens._
import parallelai.sot.api.model.{IdGenerator99UniqueSuffix, Product, RegisteredProduct, SharedSecret, Token}
import parallelai.sot.api.services.{LicenceService, OrganisationService}

class LicenceEndpointsSpec extends WordSpec with MustMatchers with IdGenerator99UniqueSuffix {
  implicit val licenceService: LicenceService = LicenceService()
  implicit val organisationService: OrganisationService = OrganisationService()

  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  val licenceErrorMessage = "Mocked Licence Error Message"

  val licenceHostExpectation: Request[_, _] => Boolean =
    _.uri.host.contains(licence.name)

  val registerProductPathExpectation: Request[_, _] => Boolean =
    _.uri.path.startsWith(Seq(licence.context, licence.version, "product", "register"))

  val bodyExpectation: Request[_, _] => Boolean =
    _.body.asInstanceOf[StringBody].s.parseJson.containsFields("code", "email", "token")

  "Product licence endpoints" should {
    "fail to register product when product token sanity check is invalid" in {
      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Result(Errors(licenceErrorMessage), Status.Unauthorized))

      new LicenceEndpoints {
        val productToken = Token("licenceId", "productCode", "productEmail")
        val product = Product("wrongCode", "wrongEmail", Option(Encrypted(productToken)))

        val Some(result) = productRegistation(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

        result.status mustEqual Status.Unauthorized
        result.value mustBe Left(Errors(licenceErrorMessage))
      }
    }

    "fail to register product when product token sanity check is invalid and no client public key was generated" in {
      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Result(Errors(licenceErrorMessage, licenceErrorMessage), Status.Unauthorized))

      new LicenceEndpoints {
        val productToken = Token("licenceId", "productCode", "productEmail")
        val product = Product("wrongCode", "wrongEmail", Option(Encrypted(productToken)))

        val Some(result) = productRegistation(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

        result.status mustEqual Status.Unauthorized
        result.value mustBe Left(Errors(licenceErrorMessage, licenceErrorMessage))
      }
    }

    "register product" in {
      val productToken = Token("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Option(Encrypted(productToken)))

      val clientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey

      val (serverPublicKey, serverSharedSecret) = DiffieHellmanServer create clientPublicKey

      val clientSharedSecret = DiffieHellmanClient.createClientSharedSecret(serverPublicKey)

      val aesSecretKey: SecretKey = Crypto.aesSecretKey

      val registeredProduct = RegisteredProduct(serverPublicKey, Encrypted(SharedSecret(productToken.id, aesSecretKey), Crypto(AES, clientSharedSecret.value)))

      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerProductPathExpectation(req) && bodyExpectation(req))
          .thenRespond(Result(registeredProduct, Status.Ok))

      new LicenceEndpoints {
        val Some(result) = productRegistation(post(p"/$productPath/register").withBody[Application.Json](product)).awaitValueUnsafe()

        result.status mustEqual Status.Ok

        val Right(registeredProduct) = result.value

        registeredProduct.serverPublicKey mustEqual serverPublicKey

        registeredProduct.apiSharedSecret.decrypt(Crypto(AES, clientSharedSecret.value)) must have(
          'id ("licenceId"),
          'secret (aesSecretKey)
        )

        licenceService.licenceId mustEqual "licenceId"
        licenceService.apiSharedSecret mustBe a [ClientSharedSecret]
      }
    }
  }
}