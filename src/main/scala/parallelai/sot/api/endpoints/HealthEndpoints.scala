package parallelai.sot.api.endpoints

import scala.concurrent.{ ExecutionContext, Future }
import io.finch._
import io.finch.syntax._
import shapeless.HNil
import spray.json.{ DefaultJsonProtocol, _ }
import com.softwaremill.sttp._
import parallelai.sot.api.actions.Response
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._

trait HealthEndpoints extends BasePath with EndpointOps with DefaultJsonProtocol {
  val healthPath: Endpoint[HNil] = basePath :: "health"

  def healthEndpoints(implicit ec: WebServiceExecutionContext, ev: SttpBackend[Future, Nothing]) = healthLicence :+: health

  protected def healthLicence(implicit ec: ExecutionContext, ev: SttpBackend[Future, Nothing]): Endpoint[Response] =
    get(healthPath :: licence.name) {
      // TODO - Remove hardcoding
      val request: Request[String, Nothing] = sttp.get(uri"http://${licence.name}:${licence.uri.getPort}/${licence.context}/2/health")
      request.send().map(r => Response(r.unsafeBody.parseJson)).toTFuture
    }

  protected def health: Endpoint[Response] =
    get(healthPath) {
      Response(s"Successfully pinged service ${api.context}").toTFuture
    }
}