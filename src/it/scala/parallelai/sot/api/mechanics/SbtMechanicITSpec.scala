package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files._
import cats.implicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import parallelai.sot.api.config._
import parallelai.sot.api.endpoints.ResponseException
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class SbtMechanicITSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with Inside
  with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(20, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  "SBT" should {
    "build a rule (fail or success) on its own thread pool" in new SbtMechanic with GoogleStorageMechanic with GitMechanic with DatastoreITConfig {
      override def changeStatus(ruleId: String, status: StatusType, statusDescription: Option[String], envId: Option[String]): Future[String] =
        "".pure[Future]

      (executor.rule.localFile("thread-rule-id") / "version").createDirectories

      whenReady(build("thread-rule-id", "version", baseDirectory / "some" / "path").failed) { t =>
        t mustBe a [ResponseException]
      }

      // TODO - assertion of thread name (only seen the correct one via a println)
    }

    "delete a rule's source code" in new SbtMechanic with GoogleStorageMechanic with GitMechanic with DatastoreITConfig {
      val ruleId = "ruleId"
      val version = "v1.0.0"
      val ruleDirectory: File = executor.rule.localFile(ruleId)
      val configDirectory: File = ruleDirectory / version / "config"

      (ruleDirectory / "target").createDirectories()
      (ruleDirectory / version / "docker").createDirectories()
      (ruleDirectory / version / ".git").createIfNotExists(asDirectory = false, createParents = true)
      (configDirectory / "dummy.json").createIfNotExists(asDirectory = false, createParents = true)

      // The only files to keep i.e. the configuration of the rule
      (configDirectory / "application.conf").createIfNotExists(asDirectory = false, createParents = true)
      (configDirectory / s"$ruleId.json").createIfNotExists(asDirectory = false, createParents = true)

      inside(deleteSource(ruleId, version).toVector.sortBy(_.name)) {
        case Vector(applicationFile, ruleFile) =>
          applicationFile.name mustEqual "application.conf"
          ruleFile.name mustEqual s"$ruleId.json"
      }
    }

    "build a rule" in {
      // TODO
    }
  }
}