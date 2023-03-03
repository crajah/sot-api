package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json.DefaultJsonProtocol
import org.joda.time.Instant.now
import parallelai.sot.api.config._
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.model.Id

trait EnvEndpoints extends EndpointOps with DefaultJsonProtocol with DatastoreMappableType {
  this: DatastoreConfig =>

  // TODO - This should not be here
  lazy val environmentDAO = datastore[Environment]

  val envPath: Endpoint[HNil] = api.path :: "env"

  lazy val envEndpoints = environment :+: environments :+: loadEnvironment :+: deleteEnvironment

  lazy val environment: Endpoint[Response] =
    get(envPath :: path[String]) { id: String =>
      environmentDAO.findOneById(id).map(Response(_)).toTFuture
    }

  lazy val environments: Endpoint[Response] =
    get(envPath) { environmentDAO.findAll.map(Response(_)).toTFuture }

  lazy val loadEnvironment: Endpoint[Response] =
    post(envPath :: jsonBody[Environment]) { request: Environment =>
      environmentDAO.put(request.copy(timestamp = Some(now))).map(env => Response(env)).toTFuture
    }

  lazy val deleteEnvironment: Endpoint[Response] =
    delete(envPath :: jsonBody[Id]) { id: Id =>
      environmentDAO.deleteById(id.value).map(u => Response(u)).toTFuture
    }
}