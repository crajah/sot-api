package parallelai.sot.api.mechanics

import cats.Id
import cats.implicits._
import org.scalatest.{ MustMatchers, WordSpec }
import com.google.api.services.dataflow.model
import com.google.api.services.dataflow.model.Job

class DataFlowProgramFutureInterpreterSpec extends WordSpec with MustMatchers {
  val withFutureFixture: DataFlowRepository[Id] = new DataFlowRepository[Id] {
    override def findAllJobs(projectId: String): Id[Seq[Job]] = {
      val jobOne = new model.Job().setId("one").setName("oneName")
      val jobTwo = new model.Job().setId("two").setName("twoName")
      Seq(jobOne, jobTwo).pure[Id]
    }

    override def findJobByRuleId(projectId: String, ruleId: String): Id[Option[Job]] = ???
  }

  "The program interpreter" should {
    "find a job for a given RuleId" in {
      val program = new DataFlowProgram(withFutureFixture)

      program.findJobByRuleId("bi-crm-poc", "oneName").get.getId mustEqual "one"
    }
  }
}