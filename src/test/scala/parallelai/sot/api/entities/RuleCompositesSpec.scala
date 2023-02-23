package parallelai.sot.api.entities

import spray.json._
import spray.json.lenses.JsonLenses._
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.sot.executor.model.SOTMacroConfig._

// TODO - Somehow will be replaced with "folders"
class RuleCompositesSpec extends WordSpec with MustMatchers with JsonFormats {
  "Rule composites" should {
    // TODO
    /*"fail to generate associated JSON because of missing required data" in {
      val dag = Dag("dag-id", "dag-name", Edge("avroSchemaFrom", "avroSchemaTo"), Edge("pubSubFrom", "pubSubTo"))

      the[DeserializationException] thrownBy {
        RuleComposites(dag).toJson
      } must have message "DAG dag-name (ID dag-id) has no associated Schemas; Taps; Steps"
    }*/

    "generate associated JSON" in {
      val dag = Dag("dag-id", "dag-name", Edge("avroSchemaFrom", "avroSchemaTo"), Edge("pubSubFrom", "pubSubTo"))

      val avroSchema = AvroSchema("avro", "avroSchemaFrom", "avroName", "v1.0.0", AvroDefinition("record", "avroName", "namespace", JsArray()))
      val pubSubTap = PubSubTapDefinition("pubsub", "pubSubFrom", "topic", None, None, None)
      val transformationOpType = TransformationOp("transformation", "id", "name", "map", Nil, paramsEncoded = false)

      val json = RuleComposites(Seq(avroSchema), Seq(pubSubTap), Seq(transformationOpType), dag.edges).toJson

      json.extract[Seq[Schema]]("schemas") mustEqual Seq(avroSchema)
      json.extract[Seq[TapDefinition]]("taps") mustEqual Seq(pubSubTap)
      json.extract[Seq[OpType]]("steps") mustEqual Seq(transformationOpType)
      json.extract[Seq[Edge]]("dag") mustEqual dag.edges
    }
  }
}