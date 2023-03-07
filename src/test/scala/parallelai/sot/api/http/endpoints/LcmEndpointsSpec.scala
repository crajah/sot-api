package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import io.finch.Input._
import io.finch.{ Input, Text }
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore.{ FromEntity, ToEntity }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }
import com.google.api.services.dataflow.model
import com.google.api.services.dataflow.model.Job
import parallelai.sot.api.model._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.mechanics.GoogleJobStatus._
import parallelai.sot.api.mechanics._

class LcmEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Lcm endpoints" should {
    "drain a job" in new LcmEndpoints with DatastoreConfig with DataflowMechanic with DataflowService {
      override def updateJob(job: Job, projectId: String, newState: JobStatus): Job = jobOne.setCurrentState(JOB_STATE_STOPPED)

      override val googleRepo = new DataFlowProgram(repoFixture)

      override lazy val lcmEnvDAO: ApiDatastore[Environment] = new ApiDatastore[Environment] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[Environment, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[Environment]] =
          Future.successful(Option(Environment("envId", "envName", EncryptedString("username_password"), "launchOpts", "projectId")))
      }

      val input: Input = put(p"/$lcmPath/stop").withBody[Text.Plain](mockBody)
      val Some(response) = lcmEndpoints(input).awaitOutputUnsafe().map(_.value)

      response.content.compactPrint mustEqual responseBody
    }
  }

  val repoFixture: DataFlowRepository[Future] = new DataFlowRepository[Future] {
    override def findAllJobs(projectId: String): Future[List[Job]] = Future(List(jobOne))
    override def findJobByRuleId(projectId: String, ruleId: String): Future[Option[Job]] = Future(Some(jobOne))
  }

  val jobOne: Job = new model.Job().setId("one").setName("oneName").setCurrentState(JOB_STATE_RUNNING)

  val responseBody: String = s"""{"currentState":"JOB_STATE_STOPPED","id":"${jobOne.getId}","name":"${jobOne.getName}"}""".stripMargin

  val mockBody: String =
    s"""
       |{
       |  "id": "${jobOne.getName}",
       |  "envId": "envId"
       |}
    """.stripMargin
}