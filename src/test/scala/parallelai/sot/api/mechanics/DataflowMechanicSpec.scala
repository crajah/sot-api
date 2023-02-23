package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ MustMatchers, WordSpec }
import com.google.api.services.dataflow.model
import com.google.api.services.dataflow.model.Job
import parallelai.sot.api.mechanics.GoogleJobStatus._

class DataflowMechanicSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  // TODO We should be stubbing only data flow api calls
  val withFutureFixture: DataFlowRepository[Future] = new DataFlowRepository[Future] {
    override def findAllJobs(projectId: String): Future[List[Job]] = List(jobOne).pure[Future]
    override def findJobByRuleId(projectId: String, ruleId: String): Future[Option[Job]] = Future(Some(jobOne))
  }

  val jobOne: Job = new model.Job().setId("one").setName("oneName").setCurrentState(JOB_STATE_RUNNING)

  "Dataflow Mechanic" should {
    "drain a Job" in new DataflowMechanic {
      override val googleRepo = new DataFlowProgram(withFutureFixture)
      override def update(job: Job, projectId: String, newState: JobStatus): Option[Job] = Some(jobOne.setCurrentState(JOB_STATE_DRAINED))

      whenReady(updateStateIfRunning("projectId", "oneName", JOB_STATE_DRAINED).value) { res =>
        res.get.getCurrentState mustEqual JOB_STATE_DRAINED
      }
    }

    "cancel a Job" in new DataflowMechanic {
      override val googleRepo = new DataFlowProgram(withFutureFixture)
      override def update(job: Job, projectId: String, newState: JobStatus): Option[Job] = Some(jobOne.setCurrentState(JOB_STATE_CANCELLED))

      whenReady(updateStateIfRunning("projectId", "oneName", JOB_STATE_CANCELLED).value) { res =>
        res.get.getCurrentState mustEqual JOB_STATE_CANCELLED
      }
    }

    "does not update state unless is running" in new DataflowMechanic {
      override val googleRepo = new DataFlowProgram(withFutureFixture)
      override def update(job: Job, projectId: String, newState: JobStatus): Option[Job] = Some(jobOne.setCurrentState(JOB_STATE_STOPPED))

      whenReady(updateStateIfRunning("projectId", "oneName", JOB_STATE_CANCELLED).value) { res =>
        res.get.getCurrentState mustEqual JOB_STATE_STOPPED
      }
    }

    "Return no job if does not find one in data flow" in new DataflowMechanic with DataFlowRepository[Future] {
      override def findAllJobs(projectId: String): Future[List[Job]] = Nil.pure[Future]
      override def findJobByRuleId(projectId: String, ruleId: String): Future[Option[Job]] = Future(None)

      override val googleRepo = new DataFlowProgram(withFutureFixture)

      whenReady(updateStateIfRunning("projectId", "oneName", JOB_STATE_CANCELLED).value) { res =>
        res.isEmpty
      }
    }
  }
}
