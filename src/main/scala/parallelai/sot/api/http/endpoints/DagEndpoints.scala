package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.OptionT
import cats.implicits._
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.datatype.datastore._
import shapeless.{Id => _, _}
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{DagActions, RuleActions}
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.model.{Id, _}

trait DagEndpoints extends EndpointOps with EntityFormats with DatastoreMappableType with RuleActions with DagActions {
  this: DatastoreConfig =>

  val dagPath: Endpoint[HNil] = api.path :: "dag"

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

object DagEndpoints {
  def apply() = (new DagEndpoints with DatastoreConfig).dagEndpoints
}