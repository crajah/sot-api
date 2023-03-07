package parallelai.sot.api.actions

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import better.files.File
import cats.implicits._
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json.JsString
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import org.slf4j.event.Level
import com.dimafeng.testcontainers.Container
import com.google.cloud.storage.Bucket
import com.twitter.finagle.http.Status
import parallelai.sot.api.config.executor
import parallelai.sot.api.http.endpoints.{Error, Response}
import parallelai.sot.api.model.{GitVersion, _}
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.mechanics._
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class RuleActionsITSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with Eventually with DatastoreMappableType
                        with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val ruleId = "my-rule-id"
  val version = "v1rc"

  def assertRuleStatus(ruleStatusDAO: DatastoreITConfig#ApiDatastore[RuleStatus], ruleId: String, status: StatusType, awaitBeforeAssertAgain: Duration = 2 seconds): Future[RuleStatus] =
    ruleStatusDAO findOneById ruleId flatMap {
      case Some(r) if r.id == ruleId && r.status == status =>
        r.pure[Future]
      case _ =>
        TimeUnit.MILLISECONDS.sleep(awaitBeforeAssertAgain.toMillis)
        assertRuleStatus(ruleStatusDAO, ruleId, status, awaitBeforeAssertAgain)
    }

  "Rule" should {
    "fail to build when bad version has been set" in new RuleActions with DatastoreITConfig {
      override protected def gitClone(statusLog: (Level, String) => Future[LogEntry], gitLocalDirectory: File): Future[LogEntry] =
        mock[LogEntry].pure[Future]

      val ruleStatus: RuleStatus = whenReady(buildRule(JsString("some json"), ruleId, version)) { response =>
        response.content.convertTo[RuleStatus]
      }

      eventually {
        whenReady(status(ruleStatus.id)) { response =>
          val ruleStatus = response.content.convertTo[RuleStatus]

          ruleStatus.status mustEqual GIT_FAILED
          ruleStatus.statusDescription.get mustEqual s"Cannot copy repository code for required version $version - following directory does not exist: ${executor.git.localFile(version).pathAsString}"
        }
      }
    }

    "fail to build when unable to get code from repository" in new RuleActions with DatastoreITConfig {
      val exception = new Exception("whoops")

      override protected def codeFromRepo: Future[File] = Future failed exception

      val ruleStatus: RuleStatus = whenReady(buildRule(JsString("some json"), ruleId, version)) { response =>
        response.content.convertTo[RuleStatus]
      }

      eventually {
        whenReady(status(ruleStatus.id)) { response =>
          response.content.convertTo[RuleStatus].status mustEqual VALIDATE_DONE // TODO - this does not convey any error - too many issues with just failing Futures
        }
      }
    }

    "fail to build because of code copying issue" in new RuleActions with DatastoreITConfig {
      // Note that a version must have been set up before trying to build a rule
      override def addVersion(version: String): Future[GitVersion] =
        executor.git.localFile(version).createDirectories.pure[Future].map(file => GitVersion(version, file.pathAsString, isActive = true))

      override protected def gitClone(statusLog: (Level, String) => Future[LogEntry], gitLocalDirectory: File): Future[LogEntry] =
        mock[LogEntry].pure[Future]

      override protected def copyRepositoryCode(ruleId: String, version: String): Future[File] = {
        executor.git.localFile(version).delete()
        super.copyRepositoryCode(ruleId, version)
      }

      val ruleStatus: RuleStatus = whenReady(addVersion(version).flatMap(_ => buildRule(JsString("some json"), ruleId, version))) { response =>
        response.content.convertTo[RuleStatus]
      }

      eventually {
        whenReady(status(ruleStatus.id)) { response =>
          val ruleStatus = response.content.convertTo[RuleStatus]

          ruleStatus.status mustEqual GIT_FAILED
          ruleStatus.statusDescription.get mustEqual s"Cannot copy repository code for required version $version - following directory does not exist: ${executor.git.localFile(version).pathAsString}"
        }
      }
    }

    "fail to build when there is a compilation error" in new RuleActions with DatastoreITConfig {
      // Note that a version must have been set up before trying to build a rule
      override def addVersion(version: String): Future[GitVersion] =
        executor.git.localFile(version).createDirectories.pure[Future].map(file => GitVersion(version, file.pathAsString, isActive = true))

      override protected def gitClone(statusLog: (Level, String) => Future[LogEntry], gitLocalDirectory: File): Future[LogEntry] =
        mock[LogEntry].pure[Future]

      whenReady {
        for {
          _ <- addVersion(version)
          buildResponse <- buildRule(JsString("some json"), ruleId, version)
          ruleStatus = buildResponse.content.convertTo[RuleStatus]
          _ = buildResponse.status mustEqual Status.Accepted
          _ <- assertRuleStatus(ruleStatusDAO, ruleStatus.id, BUILD_FAILED)
          logEntries <- logEntryDAO.findAll
          _ = println(logEntries.mkString("\n"))
        } yield logEntries.exists { logEntry =>
          logEntry.ident.exists(_.contains(ruleStatus.id)) && logEntry.msg.contains(BUILD_FAILED.toString)
        }
      } { buildFailed => buildFailed mustBe true }
    }

    "be built and cleanup after" in new RuleActions with DatastoreITConfig {
      // Note that a version must have been set up before trying to build a rule
      override def addVersion(version: String): Future[GitVersion] =
        executor.git.localFile(version).createDirectories.pure[Future].map(file => GitVersion(version, file.pathAsString, isActive = true))

      whenReady {
        for {
          _ <- addVersion(version)
          buildResponse <- buildRule(JsString("some json"), ruleId, version)
          ruleStatus = buildResponse.content.convertTo[RuleStatus]
          _ = buildResponse.status mustEqual Status.Accepted
          _ <- assertRuleStatus(ruleStatusDAO, ruleStatus.id, BUILD_FAILED)
        } yield ()
      } { _ =>
        (executor.rule.git.localFile(ruleId) / version).children.toVector.sortBy(_.name) match {
          case Vector(applicationFile, ruleFile) =>
            applicationFile.name mustEqual "application.conf"
            ruleFile.name mustEqual s"$ruleId.json"
        }
      }
    }

    "not be launched when it is busy" in new RuleActions with DatastoreITConfig {
      for {
        _ <- ruleInfoDAO insert RuleInfo(ruleId, Option("name"), version, Option("description"), "code-directory")
        _ <- ruleStatusDAO insert RuleStatus(ruleId, status = START)
        _ <- ruleBusyDAO insert RuleBusy(ruleId, busy = true)
        response <- launchRule(RuleLcm(ruleId, "envId"))
        _ = response.status mustEqual Status.Accepted
        _ <- assertRuleStatus(ruleStatusDAO, ruleId, BUILD_FAILED)
      } yield ()
    }

    "fail to get status for a non existing rule" in new RuleActions with DatastoreITConfig {
      val ruleId = "badRuleId"

      whenReady(status(ruleId)) { response =>
        response mustEqual Response(Error(s"Non existing rule: $ruleId"), Status.NotFound)
      }
    }

    "launch when it is not busy" in new RuleActions with DatastoreITConfig {
      val runLogEntry = LogEntry(ident = Option(""), stage = "", level = "", msg = ruleId)

      override protected def downloadFile(ruleId: String, bucket: Bucket): Future[String] = "blah".pure[Future]

      override def launch(ruleLcm: RuleLcm): Future[Either[Error, LogEntry]] = Right(runLogEntry).pure[Future]

      for {
        _ <- ruleInfoDAO insert RuleInfo(ruleId, Option("name"), version, Option("description"), "code-directory")
        _ <- ruleStatusDAO insert RuleStatus(ruleId, status = START)
        _ <- ruleBusyDAO insert RuleBusy(ruleId, busy = false)
        response <- launchRule(RuleLcm(ruleId, "envId"))
        _ = response.status mustEqual Status.Accepted
        _ <- assertRuleStatus(ruleStatusDAO, ruleId, LAUNCH_START)
      } yield ()
    }
  }
}