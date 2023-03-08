package parallelai.sot.api.model

import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.sot.executor.model.SOTMacroConfig.TapDefinition

case class TapWrapper(id: String, tapJson: JsValue) {
  def tap(implicit ev: JsonFormat[TapDefinition]): TapDefinition = ev read tapJson
}

object TapWrapper {
  implicit val rootJsonFormat: RootJsonFormat[TapWrapper] = jsonFormat2(TapWrapper.apply(_: String, _: JsValue))

  def apply(id: String, tap: TapDefinition)(implicit ev: JsonFormat[TapDefinition]) = new TapWrapper(id, ev write tap)
}