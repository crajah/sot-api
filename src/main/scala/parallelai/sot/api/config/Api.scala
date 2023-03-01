package parallelai.sot.api.config

import java.net.URI
import io.finch._
import shapeless.HNil

case class Api(name: String, context: String, version: String, uri: URI, ssl: Boolean) {
  lazy val path: Endpoint[HNil] = context :: version
}