package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.syntax._
import shapeless.HNil
import spray.json.{DefaultJsonProtocol, _}
import com.softwaremill.sttp._
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._

class HealthEndpoints(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with LicenceEndpointOps with DefaultJsonProtocol with Logging {
  val healthPath: Endpoint[HNil] = api.path :: "health"

  lazy val healthEndpoints = licenceHealth :+: health

  protected lazy val licenceHealth: Endpoint[Response] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    get(healthPath :: licence.context) {
      val request: Request[String, Nothing] = sttp get licenceUri"/health"
      request.send.map(r => Response(r.unsafeBody.parseJson)).toTFuture
    }
  }

  protected lazy val health: Endpoint[Response] =
    get(healthPath) {
      Response(s"Successfully pinged service ${api.name}").toTFuture
    }
}

object HealthEndpoints {
  def apply()(implicit sb: SttpBackend[Future, Nothing]) =
    (new HealthEndpoints).healthEndpoints
}