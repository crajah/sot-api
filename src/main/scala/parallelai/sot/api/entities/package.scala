package parallelai.sot.api

import spray.json._
import org.joda.time.Instant

package object entities {
  implicit val instantJsonFormat: JsonFormat[Instant] = new JsonFormat[Instant] {
    def write(obj: Instant) = JsString(obj.toString)

    def read(json: JsValue): Instant = json match {
      case JsString(dt) => Instant.parse(dt)
      case _ => throw new UnsupportedOperationException("Time Instants can only be a String")
    }
  }
}