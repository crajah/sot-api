package parallelai.sot.api.config

import java.net.URI

case class Licence(name: String, context: String, version: String, uri: URI, port: Int, ssl: Boolean, apiKey: Option[String])