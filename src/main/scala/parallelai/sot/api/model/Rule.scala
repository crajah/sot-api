package parallelai.sot.api.model

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.lenses.JsonLenses._
import org.joda.time.Instant
import org.joda.time.Instant.now
import parallelai.common.persist.Identity
import parallelai.sot.api.mechanics.StatusType
import parallelai.sot.executor.model.SOTMacroConfig._

/*
case class Rule(id: String, name: String, dagId: String, timestamp: Option[Instant] = Option(now)) extends TimedBaseEntity

object Rule {
  implicit val rootJsonFormat: RootJsonFormat[Rule] = jsonFormat4(Rule.apply)
}
*/

case class Rule(id: String, version: String, organisation: Option[String])

object Rule {
  implicit val rootJsonFormat: RootJsonFormat[Rule] = jsonFormat3(Rule.apply)
}

// TODO status: StatusType
case class RuleState(id: String, name: String, status: String, envId: String, version: String, timestamp: Option[Instant] = Option(now)) extends TimedBaseEntity

object RuleState {
  implicit val rootJsonFormat: RootJsonFormat[RuleState] = jsonFormat6(RuleState.apply)
}

// TODO
// Oldies
/*case class RuleData(name: String)

object RuleData {
  implicit val rootJsonFormat: RootJsonFormat[RuleData] = jsonFormat1(RuleData.apply)
}*/

case class RuleStatus(id: String, status: StatusType, statusDescription: Option[String] = None, envId: Option[String] = None)

object RuleStatus {
  implicit val ruleStatusIdentity: Identity[RuleStatus] = Identity[RuleStatus](_.id)

  implicit val rootJsonFormat: RootJsonFormat[RuleStatus] = jsonFormat(RuleStatus.apply, "rule-id", "status", "statusDescription", "envId")
}

case class RuleBusy(id: String, busy: Boolean)

object RuleBusy {
  implicit val ruleBusyIdentity: Identity[RuleBusy] = Identity[RuleBusy](_.id)

  implicit val rootJsonFormat: RootJsonFormat[RuleBusy] = jsonFormat2(RuleBusy.apply)
}

case class RuleInfo(id: String, name: Option[String] = None, version: String, description: Option[String] = None, codeDirectory: String)

object RuleInfo {
  implicit val ruleInfoIdentity: Identity[RuleInfo] = Identity[RuleInfo](_.id)

  implicit val rootJsonFormat: RootJsonFormat[RuleInfo] = jsonFormat5(RuleInfo.apply)
}

/**
 * Used for lifecycle management control.
 */
case class RuleLcm(id: String, envId: String)

object RuleLcm {
  implicit val rootJsonFormat: RootJsonFormat[RuleLcm] = jsonFormat2(RuleLcm.apply)
}

// TODO - Folders should replace this
case class RuleComposites(schemas: Seq[Schema] = Nil, taps: Seq[TapDefinition] = Nil, opTypes: Seq[OpType] = Nil, dag: Seq[Edge] = Nil)

object RuleComposites extends JsonFormats {
  implicit val rootJsonFormat: RootJsonFormat[RuleComposites] = new RootJsonFormat[RuleComposites] {
    def write(rs: RuleComposites): JsValue = JsObject(
      "schemas" -> rs.schemas.toJson,
      "taps" -> rs.taps.toJson,
      "steps" -> rs.opTypes.toJson,
      "dag" -> rs.dag.toJson)

    def read(json: JsValue): RuleComposites =
      RuleComposites(
        json.extract[Seq[Schema]]('rule / 'schemas),
        json.extract[Seq[TapDefinition]]('rule / 'taps),
        json.extract[Seq[OpType]]('rule / 'steps),
        json.extract[Seq[Edge]]('rule / 'dag))
  }
}