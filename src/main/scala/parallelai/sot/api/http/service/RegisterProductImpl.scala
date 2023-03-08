package parallelai.sot.api.http.service

import scala.collection.mutable
import scala.concurrent.Future
import cats.implicits._
import monocle.macros.syntax.lens._
import com.softwaremill.sttp.{Request, SttpBackend, sttp}
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.common.secure.{AES, CryptoMechanic}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.http.{Errors, Result, ResultOps}
import parallelai.sot.api.model.{ApiSharedSecret, Product, RegisteredProduct}

class RegisterProductImpl(implicit sb: SttpBackend[Future, Nothing]) extends RegisterProduct[Future] with LicenceEndpointOps with ResultOps {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  // TODO - Remove this (mutable) map and use some persistence mechanism
  val licences: mutable.Map[String, ClientSharedSecret] = mutable.Map[String, ClientSharedSecret]() // TODO - Instead of String, should have LicenceId

  def apply(pr: Product): Future[Result[RegisteredProduct]] = {
    val product = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

    val request: Request[Result[RegisteredProduct], Nothing] =
      sttp post licenceUri"/product/register" body product response asJson[Result[RegisteredProduct]]

    request.send.map { response =>
      response.body match {
        case Right(result @ Result(Right(registeredProduct), status)) =>
          val clientSharedSecret: ClientSharedSecret = createClientSharedSecret(registeredProduct.serverPublicKey)
          val apiSharedSecret: ApiSharedSecret = registeredProduct.apiSharedSecret.decrypt

          licences += (apiSharedSecret.licenceId -> clientSharedSecret)

          result

        case Right(result @ Result(Left(errors), status)) =>
          result

        case Left(error) =>
          Result(Left(Errors(error)), Status.UnprocessableEntity)
      }
    }
  }

  protected def createClientPublicKey: ClientPublicKey =
    DiffieHellmanClient.createClientPublicKey

  private def createClientSharedSecret(serverPublicKey: ServerPublicKey) =
    DiffieHellmanClient.createClientSharedSecret(serverPublicKey)
}