package parallelai.sot.api.http.service

import scala.concurrent.Future
import cats.implicits._
import com.softwaremill.sttp.{Request, SttpBackend, sttp}
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.common.secure.{AES, Crypto}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.http.{Errors, Result, ResultOps}
import parallelai.sot.api.model.{Product, RegisteredProduct}
import parallelai.sot.api.services.LicenceService
import monocle.macros.syntax.lens._

class RegisterProductImpl(implicit licenceService: LicenceService, sb: SttpBackend[Future, Nothing]) extends RegisterProduct[Future] with LicenceEndpointOps with ResultOps {
  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  def apply(product: Product): Future[Result[RegisteredProduct]] = {
    val request: Request[Result[RegisteredProduct], Nothing] =
      sttp post licenceUri"/product/register" body withClientPublicKey(product) response asJson[Result[RegisteredProduct]]

    request.send.map { response =>
      response.body match {
        case Right(result @ Result(Right(registeredProduct), _)) =>
          // licenceService.licenceId = registeredProduct.apiSharedSecret.decrypt.id TODO - Was originally this
          licenceService.apiSharedSecret = createClientSharedSecret(registeredProduct.serverPublicKey)
          licenceService.licenceId = registeredProduct.apiSharedSecret.decrypt(Crypto(AES, licenceService.apiSharedSecret.value)).id
          println(s"API: licenceId = ${licenceService.licenceId}")

          result

        case Right(result @ Result(Left(errors), status)) =>
          result

        case Left(error) =>
          Result(Left(Errors(error)), Status.UnprocessableEntity)
      }
    }
  }

  protected def withClientPublicKey(product: Product): Product = {
    product.lens(_.clientPublicKey).set(Option(DiffieHellmanClient createClientPublicKey))
  }

  protected def createClientSharedSecret(serverPublicKey: ServerPublicKey): ClientSharedSecret =
    DiffieHellmanClient createClientSharedSecret serverPublicKey
}