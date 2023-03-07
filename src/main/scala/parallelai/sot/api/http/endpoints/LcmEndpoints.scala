package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.data.OptionT
import cats.implicits._
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json.DefaultJsonProtocol
import com.twitter.finagle.http.Status
import parallelai.sot.api.config._
import parallelai.sot.api.model.Job._
import parallelai.sot.api.model._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.mechanics.DataflowMechanic
import parallelai.sot.api.mechanics.GoogleJobStatus._

trait LcmEndpoints extends DataflowMechanic with EndpointOps with DefaultJsonProtocol with DatastoreMappableType {
  this: DatastoreConfig =>

  // TODO - This should not be here
  lazy val lcmEnvDAO = datastore[Environment]

  val lcmPath: Endpoint[HNil] = api.path :: "rule"

  val lcmEndpoints: Endpoint[Response] =
    put(lcmPath :: "stop" :: jsonBody[RuleLcm] :: paramOption("cancel")) { (ruleLcm: RuleLcm, cancel: Option[String]) =>
      buildResponse(ruleLcm, cancel).value.map(opt => opt.fold(Response("Unable to stop job. Job not found or not running.", Status.BadRequest))(r => r)).toTFuture
    }

  private def buildResponse(ruleLcm: RuleLcm, cancel: Option[String]): OptionT[Future, Response] = for {
    env <- OptionT(lcmEnvDAO findOneById ruleLcm.envId)
    job <- updateStateIfRunning(env.projectId, ruleLcm.id, cancel.fold(JOB_STATE_DRAINED)(_ => JOB_STATE_CANCELLED))
  } yield Response(job.toSotJob, Status.Accepted)
}