package parallelai.sot.api.http.service

import scala.concurrent.Future
import cats.Id
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{ClientPublicKey, DiffieHellmanClient, DiffieHellmanServer}
import parallelai.common.secure.{AES, Crypto, CryptoMechanic, Encrypted}
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.model._

class RegisterProductImplSpec extends WordSpec with MustMatchers with ScalaFutures with IdGenerator99UniqueSuffix {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  val clientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey

  val (serverPublicKey, serverSharedSecret) = DiffieHellmanServer.create(clientPublicKey)

  val registeredProduct = RegisteredProduct(serverPublicKey, Encrypted(SharedSecret(uniqueId(), Crypto.aesSecretKey)))

  class RegisterProductSimpleImpl extends RegisterProduct[Id] {
    def apply(product: Product): Id[Result[RegisteredProduct]] =
      Result(Right(registeredProduct), Status.Ok)
  }

  class RegisterProductSimpleErrorImpl extends RegisterProduct[Id] {
    def apply(product: Product): Id[Result[RegisteredProduct]] =
      Result(Left(Errors("Whoops")), Status.BadRequest)
  }

  "Registration of a product" should {
    "be successful" in {
      val productToken = Token("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Option(Encrypted(productToken)))

      val registeredProduct = RegisteredProduct(serverPublicKey, Encrypted(SharedSecret(productToken.id, Crypto.aesSecretKey)))

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(_ => true)
          .thenRespond(Result(registeredProduct, Status.Ok))
      }

      val registerProduct = new RegisterProductImpl

      val result: Future[Result[RegisteredProduct]] = registerProduct(product)

      whenReady(result) { r =>
        r.status mustEqual Status.Ok
        r.value.right.get mustEqual registeredProduct

        registerProduct.licenceId mustEqual "licenceId"
      }
    }
  }

  "Simple registration of a product" should {
    "be successful" in {
      val registerProduct = new RegisterProductSimpleImpl

      val productToken = Token("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Option(Encrypted(productToken)))

      val result: Id[Result[RegisteredProduct]] = registerProduct(product)

      result.status mustEqual Status.Ok
      result.value mustBe a [Right[_, _]]
    }

    "fail" in {
      val registerProduct = new RegisterProductSimpleErrorImpl

      val productToken = Token("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Option(Encrypted(productToken)))

      val result: Id[Result[RegisteredProduct]] = registerProduct(product)

      result.status mustEqual Status.BadRequest
      result.value mustBe Left(Errors("Whoops"))
    }
  }
}