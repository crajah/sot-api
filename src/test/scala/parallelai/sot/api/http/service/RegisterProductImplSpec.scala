package parallelai.sot.api.http.service

import scala.concurrent.Future
import cats.Id
import org.scalatest.{MustMatchers, WordSpec}
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{DiffieHellmanClient, DiffieHellmanServer}
import parallelai.common.secure.{AES, CryptoMechanic, Encrypted}
import parallelai.sot.api.config.secret
import parallelai.sot.api.endpoints.{Error, Response}
import parallelai.sot.api.model.{Product, ProductToken}

class RegisterProductImplSpec extends WordSpec with MustMatchers {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  "Registration of a product" should {
    "" in {
      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => true)
          .thenRespond(Result(RegisteredProduct(DiffieHellmanServer.create(DiffieHellmanClient.createClientPublicKey)._1), Status.Ok))
      }

      val registerProduct = new RegisterProductImpl

      val productToken = ProductToken("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Encrypted(productToken))

      registerProduct(product)
    }

    "x" in {
      val registerProduct = new RegisterProductImpl2

      val productToken = ProductToken("licenceId", "productCode", "productEmail")
      val product = Product(productToken.code, productToken.email, Encrypted(productToken))

      registerProduct(product)
    }
  }
}

class RegisterProductImpl2 extends RegisterProduct[Id] {
  def apply(product: Product): Id[Result[RegisteredProduct]] = {
    println("Nice - got errors")
    Result(Left(Errors("blah")), Status.BadRequest)
  }

}