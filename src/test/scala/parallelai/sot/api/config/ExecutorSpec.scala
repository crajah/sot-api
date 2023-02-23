package parallelai.sot.api.config

import org.scalatest.{ MustMatchers, WordSpec }

class ExecutorSpec extends WordSpec with MustMatchers {
  "Configuration of Executor" should {
    "configure DAO prefix" in {
      executor.dao.prefix mustEqual "test"
    }

    "configure launch" in {
      executor.launch.className mustEqual "parallelai.sot.executor.builder.SOTBuilder"
    }
  }
}
