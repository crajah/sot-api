package parallelai.sot.api.endpoints

import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore._
import spray.json.{JsObject, JsString, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.twitter.finagle.http.Status
import parallelai.sot.api.entities.EntityFormats
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.identity.IdGenerator99UniqueSuffix
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.time.Time2016
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture
import parallelai.sot.executor.model.SOTMacroConfig._

class SchemaEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures
                            with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                            with EndpointOps with DatastoreMappableType with EntityFormats {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val avroSchema = JsObject(
    "type" -> JsString("avro"),
    "id" -> JsString("id"),
    "name" -> JsString("avroschema1"),
    "version" -> JsString("v1.0.0"),
    "definition" -> JsObject(
      "type" -> JsString("record"),
      "name" -> JsString("Message"),
      "namespace" -> JsString("parallelai.sot.avro"),
      "fields" -> JsArray())
  )

  "Schemas endpoints" should {
    "fail to register schema upon receiving an unrecognised schema" in new SchemaEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val avroSchemaWithMissingType: JsValue = avroSchema - "type"

      val throwable: Throwable = intercept[Throwable] {
        schemaEndpoints(post(p"/$schemaPath").withBody[Application.Json](avroSchemaWithMissingType)).awaitValueUnsafe()
      }

      throwable.getCause must (be (a [DeserializationException]) and have message "Schema expected")
    }

    "fail to register schema upon receiving an invalid avro schema" in new SchemaEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val avroSchemaWithMissingVersion: JsValue = avroSchema - "version"

      val throwable: Throwable = intercept[Throwable] {
        schemaEndpoints(post(p"/$schemaPath").withBody[Application.Json](avroSchemaWithMissingVersion)).awaitValueUnsafe()
      }

      throwable.getCause must (be (a [DeserializationException]) and have message "Avro schema expected")
    }

    "create a new avro schema with an ID" in new SchemaEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val Some(response) = registerSchema(post(p"/$schemaPath").withBody[Application.Json](avroSchema)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      response.content.convertTo[AvroSchema] must matchPattern {
        case AvroSchema("avro", "id", "avroschema1", "v1.0.0", _) =>
      }

      whenReady(schemaDAO findOneById "id") { registeredSchemaWrapper =>
        registeredSchemaWrapper.map(_.schema).get must have (
          'type ("avro"),
          'id ("id"),
          'name ("avroschema1"),
          'version ("v1.0.0")
        )
      }
    }

    "create a new avro schema without an ID" in new SchemaEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val avroSchemaWithoutId: JsValue = avroSchema - "id"

      val Some(response) = registerSchema(post(p"/$schemaPath").withBody[Application.Json](avroSchemaWithoutId)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      response.content.convertTo[AvroSchema] must matchPattern {
        case AvroSchema("avro", "avroschema1-99", "avroschema1", "v1.0.0", _) =>
      }

      whenReady(schemaDAO findOneById "avroschema1-99") { registeredSchemaWrapper =>
        registeredSchemaWrapper.map(_.schema).get must have (
          'type ("avro"),
          'id ("avroschema1-99"),
          'name ("avroschema1"),
          'version ("v1.0.0")
        )
      }
    }
  }
}