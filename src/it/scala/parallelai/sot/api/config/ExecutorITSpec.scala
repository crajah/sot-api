package parallelai.sot.api.config

import scala.collection.JavaConverters._
import better.files._
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.api.environment.EnvironmentHacker

class ExecutorITSpec extends WordSpec with MustMatchers with EnvironmentHacker {
  "Configuration of Executor" should {
    "configure DAO prefix" in {
      executor.dao.prefix mustEqual "it"
    }

    "configure Git" in {
      File(executor.git.localPath) mustEqual executor.directory / "git"
    }

    // TODO - This example is tad fragile
    "configure rule" in {
      val ruleId = "my-rule-id"

      executor.rule.jarFile(ruleId, "version") mustEqual File(executor.rule.localPath) / ruleId / "version" / executor.rule.jar.path.toString / executor.rule.jar.fileName

      executor.rule.jarStageFile(ruleId, "version") mustEqual File(executor.rule.localStagePath) / "version" / s"$ruleId.jar"
    }

    "configure google" in {
      executor.google.projectId mustEqual "bi-crm-poc"
    }

    "configure opts" in {
      executor.launch.opts must include ("--project=bi-crm-poc")
      executor.launch.opts must include ("--region=europe-west1")
    }

    "be overridden by environment variable" in {
      setEnv(Map("SECRET" -> "boo").asJava)
      val secretFromEnv: String = load[String]("secret")

      secretFromEnv mustEqual "boo"
    }

    "be overridden by system property" in {
      System.setProperty("secret", "boo")
      val secretFromEnv: String = load[String]("secret")

      secretFromEnv mustEqual "boo"
    }
  }
}