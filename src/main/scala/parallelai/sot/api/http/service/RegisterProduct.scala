package parallelai.sot.api.http.service

import scala.concurrent.Future

import cats.Monad
import com.softwaremill.sttp.{Request, SttpBackend, sttp}
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.sot.api.endpoints.{LicenceEndpointOps, Response}
import parallelai.sot.api.model.Product
import monocle.macros.syntax.lens._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import com.twitter.finagle.http.Status
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import cats.implicits._

abstract class RegisterProduct[F[_]: Monad] {
  def apply(product: Product): F[Result[RegisteredProduct]]
}

case class RegisteredProduct(serverPublicKey: ServerPublicKey)

object RegisteredProduct {
  implicit val encoder: Encoder[RegisteredProduct] = deriveEncoder
  implicit val decoder: Decoder[RegisteredProduct] = deriveDecoder
}

class RegisterProductImpl(implicit sb: SttpBackend[Future, Nothing]) extends RegisterProduct[Future] with LicenceEndpointOps with ResultOps {
  //implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

  def apply(pr: Product): Future[Result[RegisteredProduct]] = {
    val product = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

    val request: Request[Result[RegisteredProduct], Nothing] =
      sttp post licenceUri"/product/register" body product response asJson[Result[RegisteredProduct]]

    request.send.map { response =>
      response.body match {
        case Right(result @ Result(Right(productRegistered), status)) =>
          val clientSharedSecret: ClientSharedSecret = createClientSharedSecret(productRegistered.serverPublicKey)
          println(s"===> x = $clientSharedSecret")
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