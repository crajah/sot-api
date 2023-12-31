package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json.{DefaultJsonProtocol, _}
import parallelai.common.persist.Identity
import parallelai.sot.api.config._
import parallelai.sot.api.model.EntityFormats
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.executor.model.SOTMacroConfig.DatastoreLookupDefinition

trait LookupEndpoints extends EndpointOps with DefaultJsonProtocol with DatastoreMappableType with EntityFormats with CollectionFormats {
  this: DatastoreConfig =>

  implicit val lookupIdentity: Identity[DatastoreLookupDefinition] = Identity[DatastoreLookupDefinition](_.id)

  lazy val lookupDAO: ApiDatastore[DatastoreLookupDefinition] = datastore[DatastoreLookupDefinition]

  val lookupPath: Endpoint[HNil] = api.path :: "lookup"

  val lookupEndpoints = lookups :+: registerLookup

  lazy val lookups: Endpoint[Response] =
    get(lookupPath) {
      lookupDAO.findAll.map(Response(_)).toTFuture
    }

  lazy val registerLookup: Endpoint[Response] =
    post(lookupPath :: jsonBody[DatastoreLookupDefinition]) { lookup: DatastoreLookupDefinition =>
      lookupDAO.insert(lookup).map(Response(_)).toTFuture
    }
}

object LookupEndpoints {
  def apply() = (new LookupEndpoints with DatastoreConfig).lookupEndpoints
}