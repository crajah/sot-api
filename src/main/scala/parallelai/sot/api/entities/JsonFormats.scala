package parallelai.sot.api.entities

import scala.collection.immutable.ListMap
import spray.json._
import parallelai.sot.api.identity.IdGenerator
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig

object JsonFormats extends JsonFormats

trait JsonFormats extends SOTMacroJsonConfig with DefaultJsonProtocol with IdGenerator {
  // TODO - Now we are using 2.12 can these be moved to the appropriate companion objects?
  val defaultSchemaJsonFormat: RootJsonFormat[Schema] = SOTMacroJsonConfig.schemaJsonFormat

  implicit override val schemaJsonFormat: RootJsonFormat[Schema] = new RootJsonFormat[Schema] {
    def write(schema: Schema): JsValue =
      defaultSchemaJsonFormat write schema

    def read(json: JsValue): Schema =
      (json.asJsObject.fields.get("id"), json.asJsObject.fields.get("name")) match {
        case (None, None) =>
          defaultSchemaJsonFormat read json << ("id", uniqueId())

        case (None, Some(JsString(name))) =>
          defaultSchemaJsonFormat read json << ("id", uniqueId(name))

        case _ =>
          defaultSchemaJsonFormat read json
      }
  }

  val defaultTapJsonFormat: RootJsonFormat[TapDefinition] = SOTMacroJsonConfig.sourceJsonFormat

  implicit override val sourceJsonFormat: RootJsonFormat[TapDefinition] = new RootJsonFormat[TapDefinition] {
    def write(tap: TapDefinition): JsValue =
      defaultTapJsonFormat write tap

    def read(json: JsValue): TapDefinition =
      (json.asJsObject.fields.get("id"), json.asJsObject.fields.get("topic")) match {
        case (None, None) =>
          defaultTapJsonFormat read json << ("id", uniqueId())

        case (None, Some(JsString(topic))) =>
          defaultTapJsonFormat read json << ("id", uniqueId(topic))

        case _ =>
          defaultTapJsonFormat read json
      }
  }

  implicit override val transformationOpFormat: RootJsonFormat[TransformationOp] = new RootJsonFormat[TransformationOp] {
    override def write(obj: TransformationOp): JsValue = obj match {
      case _: TransformationOp => JsObject(
        "type" -> JsString(obj.`type`),
        "id" -> JsString(obj.id),
        "name" -> JsString(obj.name),
        "op" -> JsString(obj.op),
        "params" -> JsArray(obj.params.map(_.toJson).toVector),
        "paramsEncoded" -> JsBoolean(obj.paramsEncoded))
    }

    // TODO add test for Id
    override def read(value: JsValue): TransformationOp = value.asJsObject.fields match {
      case m: Map[String, JsValue] =>
        TransformationOp(m("type").convertTo[String], IdGenerator.uniqueId(), m("name").convertTo[String], m("op").convertTo[String],
          m("params").convertTo[Seq[Seq[String]]], m("paramsEncoded").convertTo[Boolean])
    }
  }

  implicit val datastoreLookupDefinitionFormat: RootJsonFormat[DatastoreLookupDefinition] = new RootJsonFormat[DatastoreLookupDefinition] {
    // implicit val datastoreFormat: RootJsonFormat[DatastoreLookupDefinition] = jsonFormat3(DatastoreLookupDefinition)

    def read(json: JsValue): DatastoreLookupDefinition = json.asJsObject.fields match {
      case m: Map[String, JsValue] => DatastoreLookupDefinition(IdGenerator.uniqueId(), m("schema").convertTo[String], m("tap").convertTo[String])
    }

    def write(obj: DatastoreLookupDefinition): JsValue = obj match {
      case _: DatastoreLookupDefinition => JsObject(
        "id" -> JsString(obj.id),
        "schema" -> JsString(obj.schema),
        "tap" -> JsString(obj.tap))
    }
  }

  implicit val edgeJsonFormat: RootJsonFormat[Edge] = jsonFormat2(Edge.apply)

  implicit val dagJsonFormat: RootJsonFormat[Dag] = new RootJsonFormat[Dag] {
    def write(dag: Dag): JsValue = JsObject(ListMap(
      "id" -> JsString(dag.id),
      "name" -> JsString(dag.name),
      "edges" -> dag.edges.toJson))

    def read(json: JsValue): Dag = (json.asJsObject.fields.get("name"), json.asJsObject.fields.get("edges")) match {
      case (maybeName, Some(edges)) =>
        val name = maybeName collect { case JsString(n) => n } getOrElse ""
        val id = uniqueId(name)
        Dag(id, name, edges.convertTo[Seq[Edge]]: _*)

      case (Some(_), _) =>
        deserializationError(s"Invalid DAG - Missing edges")

      case _ =>
        deserializationError(s"Invalid DAG - Missing name and edges")
    }
  }
}