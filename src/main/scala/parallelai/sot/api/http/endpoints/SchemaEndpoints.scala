package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.data.OptionT
import cats.implicits._
import grizzled.slf4j.Logging
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.datatype.datastore._
import shapeless.{Id => _, _}
import spray.json._
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.DagActions
import parallelai.sot.api.config._
import parallelai.sot.api.model._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.model.{Id, IdGenerator}
import parallelai.sot.api.time.Time
import parallelai.sot.executor.model.SOTMacroConfig._

trait SchemaEndpoints extends EndpointOps with DagActions with IdGenerator with EntityFormats with DatastoreMappableType with Time with Logging {
  this: DatastoreConfig =>

  val schemaPath: Endpoint[HNil] = api.path :: "schema"

  val schemaEndpoints = schema :+: schemas :+: registerSchema :+: deleteSchema

  lazy val schema: Endpoint[Response] =
    get(schemaPath :: path[String]) { id: String => getSchemaById(id).toTFuture }

  lazy val schemas: Endpoint[Response] = get(schemaPath) { getAllSchema.toTFuture }

  lazy val registerSchema: Endpoint[Response] =
    post(schemaPath :: jsonBody[Schema]) { schema: Schema =>
      info(s"Registering schema: $schema")
      schemaDAO.insert(SchemaWrapper(schema.id, schema)).map(s => Response(s.schema)).toTFuture
    }

  lazy val deleteSchema: Endpoint[Response] =
    delete(schemaPath :: jsonBody[Id]) { id: Id =>
      schemaDAO.deleteById(id.value).map(_ => Response(JsObject())).toTFuture
    }

  private def getAllSchema =
    schemaDAO.findAll.map(schemas => Response(schemas.map(_.schema)))

  private def getSchemaById(id: String): Future[Response] = {
    val schemaT: OptionT[Future, SchemaWrapper] = OptionT(schemaDAO.findOneById(id))
    schemaT.fold(Response(Error(s"Non existing schema: $id - Cannot proceed."), Status.NotFound))(wrapper => Response(wrapper.schema))
  }
}