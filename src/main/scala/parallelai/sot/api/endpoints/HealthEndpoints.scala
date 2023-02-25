package parallelai.sot.api.endpoints

import scala.concurrent.{ ExecutionContext, Future }
import grizzled.slf4j.Logging
import io.finch._
import io.finch.syntax._
import shapeless.HNil
import spray.json.{ DefaultJsonProtocol, _ }
import com.softwaremill.sttp._
import parallelai.sot.api.actions.Response
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._

trait HealthEndpoints extends BasePath with EndpointOps with DefaultJsonProtocol with Logging {
  val healthPath: Endpoint[HNil] = basePath :: "health"

  def healthEndpoints(implicit ec: WebServiceExecutionContext, ev: SttpBackend[Future, Nothing]) = licenceHealth :+: health

  protected def licenceHealth(implicit ec: ExecutionContext, ev: SttpBackend[Future, Nothing]): Endpoint[Response] =
    get(healthPath :: licence.context) {
      // TODO - Remove hardcoding
      val licenceHealthEndpoint = s"http://${licence.name}:${licence.port}/${licence.context}/2/health"
      info(s"${api.name} pinging service ${licence.name} via URI $licenceHealthEndpoint")

      val request: Request[String, Nothing] = sttp.get(uri"$licenceHealthEndpoint")
      request.send().map(r => Response(r.unsafeBody.parseJson)).toTFuture
    }

  protected def health: Endpoint[Response] =
    get(healthPath) {
      Response(s"Successfully pinged service ${api.name}").toTFuture
    }
}