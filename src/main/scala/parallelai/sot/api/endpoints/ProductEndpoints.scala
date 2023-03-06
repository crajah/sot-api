package parallelai.sot.api.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import monocle.macros.syntax.lens._
import shapeless.HNil
import spray.json._
import com.softwaremill.sttp._
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.common.secure.{AES, CryptoMechanic}
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.model.Product

trait ProductEndpoints extends EndpointOps with LicenceEndpointOps with ResponseOps with DefaultJsonProtocol with Logging {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  val productPath: Endpoint[HNil] = api.path :: "product"

  def productEndpoints(implicit sb: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit sb: SttpBackend[Future, Nothing]): Endpoint[Response] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    post(productPath :: "register" :: jsonBody[Product]) { pr: Product =>
      val productRegister = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

      val request: Request[Response, Nothing] =
        sttp post licenceUri"/product/register" body productRegister response asJson[Response]

      request.send.map { response =>
        response.body match {
          case Right(r) =>
            val serverPublicKey = r.content.convertTo[ServerPublicKey]
            val clientSharedSecret: ClientSharedSecret = createClientSharedSecret(serverPublicKey)
            println(s"===> x = $clientSharedSecret")

            Response(serverPublicKey)

          case Left(e) =>
            e.parseJson.convertTo[Response]
        }
      }.toTFuture
    }
  }

  protected def createClientPublicKey: ClientPublicKey =
    DiffieHellmanClient.createClientPublicKey

  private def createClientSharedSecret(serverPublicKey: ServerPublicKey) =
    DiffieHellmanClient.createClientSharedSecret(serverPublicKey)
}