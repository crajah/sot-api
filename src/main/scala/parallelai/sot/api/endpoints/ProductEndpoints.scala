package parallelai.sot.api.endpoints

import scala.concurrent.{ ExecutionContext, Future }
import grizzled.slf4j.Logging
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import monocle.macros.syntax.lens._
import shapeless.HNil
import spray.json._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import parallelai.common.secure.{ CryptoMechanic, DiffeHellmanClient, Encrypted }
import parallelai.sot.api.actions.Response
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.entities.ProductRegister

trait ProductEndpoints extends BasePath with EndpointOps with DefaultJsonProtocol with DiffeHellmanClient with Logging {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = secret.getBytes)

  val productPath: Endpoint[HNil] = basePath :: "product"

  def productEndpoints(implicit ec: WebServiceExecutionContext, ev: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit ec: ExecutionContext, ev: SttpBackend[Future, Nothing]): Endpoint[Response] =
    post(productPath :: "register" :: jsonBody[ProductRegister]) { pr: ProductRegister =>
      val productRegister = pr.lens(_.dhkeClientPublicKey) set Option(Encrypted(clientPublicKey))

      val request: Request[String, Nothing] =
        sttp post uri"http://${licence.name}:${licence.port}/${licence.context}/${licence.version}/product/register?key=${licence.apiKey}" body productRegister

      request.send.map(r => Response(r.unsafeBody.parseJson)).toTFuture
    }
}