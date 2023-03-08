package parallelai.sot.api.model

import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.sot.executor.model.SOTMacroConfig.OpType

case class OpTypeWrapper(id: String, opTypeJson: JsValue) {
  def opType(implicit ev: JsonFormat[OpType]): OpType = ev read opTypeJson
}

object OpTypeWrapper {
  implicit val rootJsonFormat: RootJsonFormat[OpTypeWrapper] = jsonFormat2(OpTypeWrapper.apply(_: String, _: JsValue))

  def apply(id: String, opType: OpType)(implicit ev: JsonFormat[OpType]) = new OpTypeWrapper(id, ev write opType)
}