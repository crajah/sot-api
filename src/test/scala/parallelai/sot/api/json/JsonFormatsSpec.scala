package parallelai.sot.api.json

import scala.collection.immutable.ListMap
import spray.json.{ JsArray, JsObject, JsString }
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.sot.api.entities.JsonFormats
import parallelai.sot.api.identity.IdGenerator99UniqueSuffix
import parallelai.sot.executor.model.SOTMacroConfig._

class JsonFormatsSpec extends WordSpec with MustMatchers with JsonFormats with IdGenerator99UniqueSuffix {
  "Json formats" should {
    val avroDefinition = JsObject(ListMap(
      "type" -> JsString("record"),
      "name" -> JsString("Message"),
      "namespace" -> JsString("parallelai.sot.avro"),
      "fields" -> JsArray()))

    "convert avro definition JSON to associated ADT" in {
      avroDefinition.convertTo[AvroDefinition] mustEqual AvroDefinition("record", "Message", "parallelai.sot.avro", JsArray())
    }

    "convert avro schema JSON that has ID to associated ADT" in {
      val avroSchema = JsObject(ListMap(
        "type" -> JsString("avro"),
        "id" -> JsString("id"),
        "name" -> JsString("avroschema1"),
        "version" -> JsString("v1.0.0"),
        "definition" -> avroDefinition))

      avroSchema.convertTo[AvroSchema] mustEqual AvroSchema("avro", "id", "avroschema1", "v1.0.0", AvroDefinition("record", "Message", "parallelai.sot.avro", JsArray()))
    }

    "convert avro schema JSON without ID to associated ADT" in {
      val avroSchema = JsObject(ListMap(
        "type" -> JsString("avro"),
        "name" -> JsString("avroschema1"),
        "version" -> JsString("v1.0.0"),
        "definition" -> avroDefinition))

      avroSchema.convertTo[Schema] mustEqual AvroSchema("avro", "avroschema1-99", "avroschema1", "v1.0.0", AvroDefinition("record", "Message", "parallelai.sot.avro", JsArray()))
    }
  }
}