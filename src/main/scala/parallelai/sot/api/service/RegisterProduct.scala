package parallelai.sot.api.service

import scala.concurrent.Future
import com.softwaremill.sttp.{Request, SttpBackend, sttp}
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.sot.api.endpoints.{LicenceEndpointOps, Response}
import parallelai.sot.api.model.Product
import monocle.macros.syntax.lens._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import parallelai.sot.api.concurrent.WebServiceExecutionContext

trait RegisterProduct {
  def apply(product: Product)(implicit sb: SttpBackend[Future, Nothing]): Future[ServerPublicKey]
}

case class ProductRegistered(x: String)

object ProductRegistered {
  implicit val statusEncoder: Encoder[ProductRegistered] = deriveEncoder
  implicit val statusDecoder: Decoder[ProductRegistered] = deriveDecoder
}

class RegisterProductImpl extends RegisterProduct with LicenceEndpointOps with ResultOps {
  implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

  def apply(pr: Product)(implicit sb: SttpBackend[Future, Nothing]): Future[ServerPublicKey] = {
    val product = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

    val request: Request[Result[ProductRegistered], Nothing] =
      sttp post licenceUri"/product/register" body product response asJson[Result[ProductRegistered]]

    request.send.map { response =>
      response.body match {
        case Right(result) => null //result
        case Left(error) => null
      }
    }

    /*request.send.map { response =>
      response.body match {
        case Right(r) =>
          val serverPublicKey = r.content.convertTo[ServerPublicKey]
          val clientSharedSecret: ClientSharedSecret = createClientSharedSecret(serverPublicKey)
          println(s"===> x = $clientSharedSecret")

          Response(serverPublicKey)

        case Left(e) =>
          e.parseJson.convertTo[Response]
      }
    }*/
  }

  protected def createClientPublicKey: ClientPublicKey =
    DiffieHellmanClient.createClientPublicKey

  private def createClientSharedSecret(serverPublicKey: ServerPublicKey) =
    DiffieHellmanClient.createClientSharedSecret(serverPublicKey)
}