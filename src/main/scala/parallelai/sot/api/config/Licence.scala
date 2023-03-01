package parallelai.sot.api.config

import java.net.URI
import com.softwaremill.sttp._

case class Licence(name: String, context: String, version: String, host: URI, port: Int, ssl: Boolean, apiKey: Option[String] = None) {
  lazy val uri: Uri = uri"http://$name:$port/$context/$version"
}