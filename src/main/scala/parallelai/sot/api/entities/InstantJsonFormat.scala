package parallelai.sot.api.entities

import spray.json._
import org.joda.time.Instant

object InstantJsonFormat extends InstantJsonFormat

trait InstantJsonFormat {
  implicit val instantJsonFormat: JsonFormat[Instant] = new JsonFormat[Instant] {
    def write(obj: Instant) = JsString(obj.toString)

    def read(json: JsValue): Instant = json match {
      case JsString(dt) => Instant.parse(dt)
      case _ => throw new UnsupportedOperationException("Time Instants can only be a String")
    }
  }
}