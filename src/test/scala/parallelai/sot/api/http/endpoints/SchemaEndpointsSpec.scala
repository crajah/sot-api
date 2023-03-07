package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import io.finch.Input._
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore.{ FromEntity, ToEntity }
import spray.json.JsArray
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.common.persist.Identity
import parallelai.sot.api.actions.DagActions
import parallelai.sot.api.model.SchemaWrapper
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.executor.model.SOTMacroConfig.{ AvroDefinition, AvroSchema }

class SchemaEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit private val schemaWrapperIdentity: Identity[SchemaWrapper] = Identity[SchemaWrapper](_.id)

  "Schema endpoints" should {
    "retrieve all schemas" in new SchemaEndpoints with DatastoreConfig with DagActions {
      override lazy val schemaDAO: ApiDatastore[SchemaWrapper] = new ApiDatastore[SchemaWrapper] {
        override def findAll[L <: HList](implicit gen: Aux[SchemaWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[SchemaWrapper]] = {
          val schemaWrappers = List(
            SchemaWrapper("id", AvroSchema("avro", "id", "avroschema1", "version2", AvroDefinition("record", "Message", "parallelai.sot.avro", JsArray()))),
            SchemaWrapper("id2", AvroSchema("avro", "id2", "avroschema1", "version2", AvroDefinition("record", "Message", "parallelai.sot.avro", JsArray()))))
          Future.successful(schemaWrappers)
        }
      }

      val Some(response) = schemas(get(p"/$schemaPath")).awaitValueUnsafe()

      val avroSchemas: List[AvroSchema] = response.content.convertTo[List[AvroSchema]]
      avroSchemas.size mustEqual 2
      avroSchemas.map(_.id) mustEqual List("id", "id2")
    }

    "retrieve a schema for a given id" in new SchemaEndpoints with DatastoreConfig with DagActions {
      override lazy val schemaDAO: ApiDatastore[SchemaWrapper] = new ApiDatastore[SchemaWrapper] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[SchemaWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[SchemaWrapper]] =
          Future.successful(Some(SchemaWrapper("id", AvroSchema("avro", "id", "avroschema1", "version2", AvroDefinition("record", "Message", "parallelai.sot.avro", JsArray())))))
      }

      val Some(response) = schema(get(p"/$schemaPath/id")).awaitValueUnsafe()

      response.content.convertTo[AvroSchema].id mustEqual "id"
    }

    "return error message if cannot be found" in new SchemaEndpoints with DatastoreConfig with DagActions {
      override lazy val schemaDAO: ApiDatastore[SchemaWrapper] = new ApiDatastore[SchemaWrapper] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[SchemaWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[SchemaWrapper]] =
          Future.successful(None)
      }

      val Some(response) = schema(get(p"/$schemaPath/anyId")).awaitValueUnsafe()

      response.content.compactPrint mustEqual """{"error-message":"Non existing schema: anyId - Cannot proceed."}"""
    }
  }
}