package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import better.files._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import parallelai.sot.api.config.executor
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class GitMechanicITSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar
  with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(20, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  "GIT" should {
    "not pull for non existing local directory" in new GitMechanic with DatastoreITConfig {
      gitPullCommitAndPush("rules-git-missing", "verson").failed.futureValue must
        (be (an [Exception]) and have message s"Git Folder: ${executor.rule.git.localPath} does not exist - No repository found")
    }

    "pull commit and push" in new GitMechanic with DatastoreITConfig {
      val `test-rule` = s"test-rule-${System.currentTimeMillis}"
      val version = "v1.0.0"

      def testRule: File = {
        val ruleFile = (File(executor.rule.git.localPath) / `test-rule` / version / "config" / s"${`test-rule`}.json").createIfNotExists(asDirectory = false, createParents = true)
        ruleFile.write(s"""{ "rule-id": "${`test-rule`}" }""")

        val applicationConfFile = (File(executor.rule.git.localPath) / `test-rule` / version / "config" / "application.conf").createIfNotExists(asDirectory = false, createParents = true)
        applicationConfFile.write(s"""{ "json.file.name": "${`test-rule`}.json" }""")
      }

      whenReady(codeFromRepo.map(_ => testRule).flatMap(_ => gitPullCommitAndPush(`test-rule`, version))) { result =>
        result.msg mustEqual s"Rule ${`test-rule`} Status changed to GIT_DONE"
      }
    }
  }
}