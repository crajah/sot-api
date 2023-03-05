package parallelai.sot.api.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import monocle.macros.syntax.lens._
import shapeless.HNil
import spray.json._
import spray.json.lenses.JsonLenses._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import parallelai.common.secure.CryptoMechanic
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.model.Product

trait ProductEndpoints extends EndpointOps with LicenceEndpointOps with DefaultJsonProtocol with Logging {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = secret.getBytes)

  val productPath: Endpoint[HNil] = api.path :: "product"

  def productEndpoints(implicit sb: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit sb: SttpBackend[Future, Nothing]): Endpoint[Response] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    post(productPath :: "register" :: jsonBody[Product]) { pr: Product =>
      val productRegister = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

      val request: Request[String, Nothing] =
        sttp post licenceUri"/product/register" body productRegister

      request.send.map { response =>
        val serverPublicKey = response.unsafeBody.parseJson.extract[ServerPublicKey]("content")
        val clientSharedSecret: ClientSharedSecret = DiffieHellmanClient.createClientSharedSecret(serverPublicKey)
        println(s"===> x = $clientSharedSecret")

        Response("")
      }.toTFuture
    }
  }

  protected def createClientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey
}