package parallelai.sot.api.endpoints

import scala.concurrent.{ ExecutionContext, Future }
import grizzled.slf4j.Logging
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import com.softwaremill.sttp._
import parallelai.sot.api.actions.Response
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.entities.ProductRegister

trait ProductEndpoints extends BasePath with EndpointOps with Logging {
  val productPath: Endpoint[HNil] = basePath :: "product"

  def productEndpoints(implicit ec: WebServiceExecutionContext, ev: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit ec: ExecutionContext, ev: SttpBackend[Future, Nothing]): Endpoint[Response] =
    post(productPath :: "register" :: jsonBody[ProductRegister]) { productRegister: ProductRegister =>
      /*val request: Request[String, Nothing] = sttp get uri"http://${licence.name}:${licence.port}/${licence.context}/${licence.version}/health?key=${licence.apiKey}"
      request.send().map(r => Response(r.unsafeBody.parseJson)).toTFuture*/

      println(s"===> ${new String(productRegister.productToken.value)}")

      Response(productRegister).toTFuture
    }
}