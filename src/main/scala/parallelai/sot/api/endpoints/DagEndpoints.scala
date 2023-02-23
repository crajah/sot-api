package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.OptionT
import cats.implicits._
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless._
import shapeless.datatype.datastore._
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{ DagActions, Response, RuleActions }
import parallelai.sot.api.entities.{ Error, Id, _ }
import parallelai.sot.api.gcp.datastore.DatastoreConfig

trait DagEndpoints extends BasePath with EndpointOps with EntityFormats with DatastoreMappableType with RuleActions with DagActions {
  this: DatastoreConfig =>

  val dagPath: Endpoint[HNil] = basePath :: "dag"

  val dagEndpoints = dag :+: dags :+: registerDag :+: deleteDag

  lazy val dag: Endpoint[Response] =
    get(dagPath :: path[String]) { id: String =>
      OptionT(dagDAO.findOneById(id))
        .fold(Response(Error(s"Non existing dag: $id - Cannot proceed."), Status.NotFound))(dag => Response(dag)).toTFuture
    }

  lazy val dags: Endpoint[Response] =
    get(dagPath) { dagDAO.findAll.map(Response(_)).toTFuture }

  lazy val registerDag: Endpoint[Response] =
    post(dagPath :: jsonBody[Dag]) { dag: Dag =>
      dagDAO.insert(dag).map(dag => Response(dag)).toTFuture
    }

  lazy val deleteDag: Endpoint[Response] =
    delete(dagPath :: jsonBody[Id]) { id: Id =>
      dagDAO.deleteById(id.value).map(u => Response(u)).toTFuture
    }
}