package parallelai.sot.api.endpoints

import io.finch._
import shapeless.HNil
import parallelai.sot.api.config.api

trait BasePath {
  val basePath: Endpoint[HNil] = api.name :: api.version
}