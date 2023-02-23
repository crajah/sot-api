package parallelai.sot.api.config

import java.net.URI

case class Api(name: String, version: String, uri: URI, ssl: Boolean)

case class Licence(name: String, version: String, uri: URI, ssl: Boolean)