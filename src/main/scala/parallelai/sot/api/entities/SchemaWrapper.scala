package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.sot.executor.model.SOTMacroConfig.Schema

case class SchemaWrapper(id: String, schemaJson: JsValue) {
  def schema(implicit ev: JsonFormat[Schema]): Schema = ev read schemaJson
}

object SchemaWrapper {
  implicit val rootJsonFormat: RootJsonFormat[SchemaWrapper] = jsonFormat2(SchemaWrapper.apply(_: String, _: JsValue))

  def apply(id: String, schema: Schema)(implicit ev: JsonFormat[Schema]) = new SchemaWrapper(id, ev write schema)
}