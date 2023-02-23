package parallelai.sot.api.entities

import shapeless.datatype.datastore.BaseDatastoreMappableType
import shapeless.datatype.datastore.DatastoreType.at
import spray.json.{ JsString, JsValue, JsonFormat }

@deprecated(message = "I believe this is no longer used", since = "2nd March 2018")
sealed trait SourceSinkType {
  def name: String
}

case object SourceType extends SourceSinkType {
  override val name = "source"
}

case object SinkType extends SourceSinkType {
  override val name = "sink"
}

object SourceSinkType {
  implicit class StringToSourceType(s: String) {
    // TODO - Not nice
    def toSourceType: SourceSinkType = s match {
      case "source" => SourceType
      case "sink" => SinkType
      case _ => throw new UnsupportedOperationException(s"Source Type name $s unknown")
    }
  }

  implicit object SourceSinkTypeJsonFormat extends JsonFormat[SourceSinkType] {
    def write(obj: SourceSinkType) = JsString(obj.toString)

    def read(json: JsValue): SourceSinkType = json match {
      case JsString(s) => s.toSourceType
      case _ => throw new UnsupportedOperationException("Source types can only be a String")
    }
  }

  implicit val entityMappableType: BaseDatastoreMappableType[SourceSinkType] = at[SourceSinkType](toE, fromE)

  private def toE(v: com.google.datastore.v1.Value): SourceSinkType = v.getStringValue.toSourceType

  private def fromE(i: SourceSinkType): com.google.datastore.v1.Value =
    com.google.datastore.v1.Value.newBuilder().setStringValue(i.name).build()
}