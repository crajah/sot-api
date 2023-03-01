package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore._
import spray.json._
import com.twitter.finagle.http.Status
import parallelai.common.persist.Identity
import parallelai.sot.api.actions.Response
import parallelai.sot.api.config._
import parallelai.sot.api.entities.{ Error, Id, _ }
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.executor.model.SOTMacroConfig._

trait StepEndpoints extends EndpointOps with DefaultJsonProtocol with DatastoreMappableType with EntityFormats {
  this: DatastoreConfig =>

  implicit val opTypeIdentity: Identity[OpType] = Identity[OpType](_.id)
  implicit val opTypeWrapperIdentity: Identity[OpTypeWrapper] = Identity[OpTypeWrapper](_.id)

  lazy val stepDAO = datastore[OpTypeWrapper] // TODO remove this

  val stepPath: Endpoint[HNil] = api.path :: "step"

  val stepEndpoints = step :+: steps :+: registerStep :+: deleteStep

  lazy val step: Endpoint[Response] =
    get(stepPath :: path[String]) { id: String =>
      (stepDAO findOneById id).map {
        case Some(s) => Response(s.opType)
        case _ => Response(Error(s"Non existing step: $id - Cannot proceed."), Status.NotFound)
      }.toTFuture
    }

  lazy val steps: Endpoint[Response] =
    get(stepPath) { stepDAO.findAll.map(steps => Response(steps.map(_.opType))).toTFuture }

  lazy val registerStep: Endpoint[Response] =
    post(stepPath :: jsonBody[OpType]) { opType: OpType =>
      stepDAO.insert(OpTypeWrapper(opType.id, opType)).map(step => Response(step.opType)).toTFuture
    }

  lazy val deleteStep: Endpoint[Response] =
    delete(stepPath :: jsonBody[Id]) { id: Id =>
      stepDAO.deleteById(id.value).map(_ => Response(JsObject())).toTFuture
    }
}