package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch.syntax._
import io.finch.{Errors => _, _}
import shapeless.HNil
import com.softwaremill.sttp._
import com.twitter.finagle.http.Status
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.http.{Errors, Result, ResultOps}

class HealthEndpoints(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with LicenceEndpointOps with ResultOps with Logging {
  val healthPath: Endpoint[HNil] = api.path :: "health"

  lazy val healthEndpoints = licenceHealth :+: health

  protected lazy val licenceHealth: Endpoint[Result[String]] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    get(healthPath :: licence.context) {
      val request: Request[Result[String], Nothing] = sttp get licenceUri"/health" response asJson[Result[String]]

      request.send.map { response =>
        response.body match {
          case Right(result) => result
          case Left(error) => Result[String](Left(Errors(error)), Status.UnprocessableEntity)
        }
      } toTFuture
    }
  }

  protected lazy val health: Endpoint[Result[String]] =
    get(healthPath) {
      Result(s"Successfully pinged service ${api.name}", Status.Ok).toTFuture
    }
}

object HealthEndpoints {
  def apply()(implicit sb: SttpBackend[Future, Nothing]) =
    (new HealthEndpoints).healthEndpoints
}