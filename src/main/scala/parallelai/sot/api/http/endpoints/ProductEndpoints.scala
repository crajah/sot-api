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

class ProductEndpoints(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with Logging {
  lazy val registerProduct = new RegisterProductImpl

  val productPath: Endpoint[HNil] = api.path :: "product"

  lazy val productEndpoints: Endpoint[Result[RegisteredProduct]] = {
    post(productPath :: "register" :: jsonBody[Product]) { product: Product =>
      val result: Future[Result[RegisteredProduct]] = registerProduct(product)

      result.toTFuture
    }
  }
}