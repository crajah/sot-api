package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import io.finch.Input._
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore._
import spray.json._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.google.api.services.dataflow.model
import com.google.api.services.dataflow.model.Job
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.twitter.finagle.http.Status
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.mechanics.GoogleJobStatus._
import parallelai.sot.api.mechanics.{DataFlowRepository, _}
import parallelai.sot.api.model._
import parallelai.sot.api.services.{LicenceService, OrganisationService, VersionService}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class RuleJobEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures with Inside with Eventually with MockitoSugar
                             with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                             with EndpointOps with DatastoreMappableType {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  implicit val licenceService: LicenceService = LicenceService()
  implicit val organisationService: OrganisationService = OrganisationService()
  implicit val versionService: VersionService = VersionService()
  implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

  val container: Container = datastoreContainer

  val stoppedJob: model.Job = new model.Job().setCurrentState(JOB_STATE_STOPPED).setName("stoppedJob")
  val runningJob: model.Job = new model.Job().setCurrentState(JOB_STATE_RUNNING).setName("runningJob")
  val runningJobTwo: model.Job = new model.Job().setCurrentState(JOB_STATE_RUNNING).setName("runningJobTwo")

  val interpreter: DataFlowRepository[Future] = new DataFlowRepository[Future] {
    override def findAllJobs(projectId: String): Future[Seq[Job]] = List(runningJobTwo, stoppedJob, runningJob).pure[Future]
    override def findJobByRuleId(projectId: String, ruleId: String): Future[Option[Job]] = ??? // TODO
  }

  "Rule endpoints for jobs" should {
    "return all running jobs" in new RuleEndpoints with DataflowMechanic with DatastoreITConfig {
      override val googleRepo: DataFlowProgram[Future] = new DataFlowProgram(interpreter)

      override lazy val ruleStatusDAO: ApiDatastore[RuleStatus] = new ApiDatastore[RuleStatus] {
        override def findAll[L <: HList](implicit gen: Aux[RuleStatus, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[RuleStatus]] =
          Future.successful(RuleStatus(stoppedJob.getName, LAUNCH_DONE) :: RuleStatus(runningJob.getName, LAUNCH_DONE) :: RuleStatus(runningJobTwo.getName, LAUNCH_DONE) :: Nil)
      }

      val Some(response) = allRule(get(p"/$rulePath")).awaitValueUnsafe()
      response.status mustEqual Status.Ok
      response.content mustEqual s"""[{"currentState":"$JOB_STATE_RUNNING","name":"runningJobTwo"},{"currentState":"$JOB_STATE_RUNNING","name":"runningJob"}]""".parseJson
    }

    "return all created jobs with 'all' param" in new RuleEndpoints with DataflowMechanic with DatastoreITConfig {
      override val googleRepo: DataFlowProgram[Future] = new DataFlowProgram(interpreter)

      override lazy val ruleStatusDAO: ApiDatastore[RuleStatus] = new ApiDatastore[RuleStatus] {
        override def findAll[L <: HList](implicit gen: Aux[RuleStatus, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[RuleStatus]] =
          Future.successful(RuleStatus(stoppedJob.getName, LAUNCH_DONE) :: RuleStatus(runningJob.getName, LAUNCH_DONE) :: Nil)
      }

      val Some(response) = allRule(get(p"/$rulePath?all")).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      response.content mustEqual s"""[{"currentState":"$JOB_STATE_STOPPED","name":"stoppedJob"},{"currentState":"$JOB_STATE_RUNNING","name":"runningJob"}]""".parseJson
    }

    "return all created jobs in lates order with 'all' and 'latest' params" in new RuleEndpoints with DataflowMechanic with DatastoreITConfig {
      override val googleRepo: DataFlowProgram[Future] = new DataFlowProgram(interpreter)
      stoppedJob setCreateTime "2018-02-16_06_17_46"
      runningJob setCreateTime "2018-02-16_06_16_38"
      runningJobTwo setCreateTime "2018-02-15_10_46_41"

      override lazy val ruleStatusDAO: ApiDatastore[RuleStatus] = new ApiDatastore[RuleStatus] {
        override def findAll[L <: HList](implicit gen: Aux[RuleStatus, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[RuleStatus]] =
          Future.successful(RuleStatus(stoppedJob.getName, LAUNCH_DONE) :: RuleStatus(runningJobTwo.getName, LAUNCH_DONE) :: RuleStatus(runningJob.getName, LAUNCH_DONE) :: Nil)
      }

      val Some(response) = allRule(get(p"/$rulePath?all&latest")).awaitValueUnsafe()

      response.status mustEqual Status.Ok

      response.content.convertTo[Seq[parallelai.sot.api.model.Job]].map(j => (j.currentState.get, j.name.get, j.createTime.get)) mustEqual
        Seq(
          (JOB_STATE_STOPPED, "stoppedJob", "2018-02-16_06_17_46"),
          (JOB_STATE_RUNNING, "runningJob", "2018-02-16_06_16_38"),
          (JOB_STATE_RUNNING, "runningJobTwo", "2018-02-15_10_46_41")
        )
    }
  }
}