package parallelai.sot.api.config

import java.net.URI

case class Api(name: String, context: String, version: String, uri: URI, ssl: Boolean = false)

case class Licence(name: String, context: String, version: String, uri: URI, port: Int, ssl: Boolean = false, apiKey: Option[String] = None)