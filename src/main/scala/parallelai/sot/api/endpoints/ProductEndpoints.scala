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
import com.softwaremill.sttp.circe._
import parallelai.common.secure.CryptoMechanic
import parallelai.common.secure.diffiehellman.{ClientPublicKey, DiffieHellmanClient}
import parallelai.sot.api.actions.Response
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.entities.ProductRegister

trait ProductEndpoints extends EndpointOps with DefaultJsonProtocol with Logging {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = secret.getBytes)

  val productPath: Endpoint[HNil] = api.path :: "product"

  def productEndpoints(implicit sb: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit sb: SttpBackend[Future, Nothing]): Endpoint[Response] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    post(productPath :: "register" :: jsonBody[ProductRegister]) { pr: ProductRegister =>
      val productRegister = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

      val request: Request[String, Nothing] =
        sttp post uri"${licence.uri}/product/register?key=${licence.apiKey}" body productRegister // TODO implicitly add the "key" as it could be easily missed out

      request.send.map { response =>
        val x = response.unsafeBody.parseJson
        println(s"===> x = ${x.prettyPrint}")

        Response(x)
      }.toTFuture
    }
  }

  protected def createClientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey
}

/*
val (serverPublicKey, serverSharedSecret) = DiffieHellmanServer.create(DiffieHellmanClient.createClientPublicKey)

val clientSharedSecret: ClientSharedSecret = DiffieHellmanClient.createClientSharedSecret(serverPublicKey)

clientSharedSecret.value mustEqual serverSharedSecret.value
 */