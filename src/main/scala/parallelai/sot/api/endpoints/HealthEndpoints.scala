package parallelai.sot.api.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.syntax._
import shapeless.HNil
import spray.json.{ DefaultJsonProtocol, _ }
import com.softwaremill.sttp._
import parallelai.sot.api.actions.Response
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._

trait HealthEndpoints extends EndpointOps with DefaultJsonProtocol with Logging {
  val healthPath: Endpoint[HNil] = api.path :: "health"

  def healthEndpoints(implicit sb: SttpBackend[Future, Nothing]) = licenceHealth :+: health

  protected def licenceHealth(implicit sb: SttpBackend[Future, Nothing]): Endpoint[Response] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    get(healthPath :: licence.context) {
      // TODO - Remove hardcoding
      val request: Request[String, Nothing] = sttp get uri"http://${licence.name}:${licence.port}/${licence.context}/${licence.version}/health?key=${licence.apiKey}"
      request.send.map(r => Response(r.unsafeBody.parseJson)).toTFuture
    }
  }

  protected def health: Endpoint[Response] =
    get(healthPath) {
      Response(s"Successfully pinged service ${api.name}").toTFuture
    }
}