package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.circe._
import io.finch.syntax._
import shapeless.HNil
import com.softwaremill.sttp._
import parallelai.sot.api.config._
import parallelai.sot.api.http.Result
import parallelai.sot.api.http.service.RegisterProductImpl
import parallelai.sot.api.model.{Product, RegisteredProduct}

trait ProductEndpoints extends EndpointOps with LicenceEndpointOps with Logging {
  val productPath: Endpoint[HNil] = api.path :: "product"

  def productEndpoints(implicit sb: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit sb: SttpBackend[Future, Nothing]): Endpoint[Result[RegisteredProduct]] = {
    val registerProduct = new RegisterProductImpl

    post(productPath :: "register" :: jsonBody[Product]) { product: Product =>
      val result: Future[Result[RegisteredProduct]] = registerProduct(product)

      result.toTFuture
    }
  }
}