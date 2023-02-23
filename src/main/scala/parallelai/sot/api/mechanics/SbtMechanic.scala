package parallelai.sot.api.mechanics

import scala.concurrent.{ ExecutionContext, Future, blocking }
import scala.sys.process._
import scala.util.Try
import better.files._
import grizzled.slf4j.Logging
import org.slf4j.event.Level._
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{ Response, ResponseException }
import parallelai.sot.api.concurrent.SbtExcecutionContext
import parallelai.sot.api.config.executor
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.DatastoreConfig

trait SbtMechanic extends StatusMechanic with Logging {
  this: GoogleStorageMechanic with GitMechanic with DatastoreConfig =>

  private implicit val ec: ExecutionContext = SbtExcecutionContext()

  def build(ruleId: String, version: String, path: File): Future[LogEntry] = blocking {
    val processLogging = new ProcessLogging(ruleId)
    val statusLog = logPartialStatus(Some(s"RULE: $ruleId"), BUILD)
    val errorLog = logPartialError(Some(s"RULE: $ruleId"), BUILD)

    (for {
      _ <- changeStatus(ruleId, BUILD_START)
      _ <- statusLog(INFO, s"Executing sbt in ${Process("pwd", path.toJava).!!}")
      sbt <- Future {
        Process(executor.sbt.command, path.toJava, "SBT_OPTS" -> executor.sbt.opts).!(processLogging) match {
          case 0 => s"Build Success for rule: $ruleId"
          case x => throw new Exception(s"Build Failed for rule: $ruleId with process code $x") // TODO - Not nice
        }
      }(ec)
      _ <- handleRuleSuccess(ruleId, BUILD_DONE, sbt)(statusLog)
      _ <- whenBusy(ruleId, moveJarToStage(ruleId, version)) // TODO moveJarToStage is doing far too much
      logEntry <- statusLog(INFO, s"For ruleId $ruleId with version $version local source code has been deleted")
    } yield {
      processLogging.flush()
      logEntry
    }).recoverWith {
      case t: Throwable =>
        deleteSource(ruleId, version)

        (for {
          _ <- handleRuleFailure(ruleId, BUILD_FAILED, t)(statusLog)(errorLog)
          _ <- errorLog(ERROR, t)
          logEntry <- statusLog(INFO, s"For ruleId $ruleId with version $version local source code has been deleted")
        } yield {
          processLogging.flush()
          logEntry
        }).flatMap { logEntry =>
          // TODO - An unfortunate consequence of the design issue of no proper contract for this function and utilising "exceptions" as part of the overall control flow
          Future failed ResponseException(Response(Error(logEntry.msg), Status.UnprocessableEntity))
        }
    }
  }

  // TODO - Tag arguments to distinguish them
  protected def deleteSource(ruleId: String, version: String): Iterator[File] = {
    val errorLog = logPartialError(Some(s"RULE: $ruleId"), GIT)

    val applicationFileName = "application.conf"
    val ruleFileName = s"$ruleId.json"
    val versionDirectory = executor.rule.git.localFile(ruleId) / version
    val configDirectory = versionDirectory / "config"

    (executor.rule.git.localFile(ruleId) / "target") delete true

    Try {
      (configDirectory / applicationFileName) moveToDirectory versionDirectory
    } recover { case t => errorLog(ERROR, t) }

    Try {
      (configDirectory / ruleFileName) moveToDirectory versionDirectory
    } recover { case t => errorLog(ERROR, t) }

    versionDirectory.children.foreach { file =>
      if (file.name != applicationFileName && file.name != ruleFileName) file.delete()
    }

    versionDirectory.children
  }

  protected def cleanProject(ruleId: String, path: File): Future[String] = Future {
    Process("sbt clean", path.toJava).!(new ProcessLogging(ruleId)) match {
      case 0 => s"Clean Success for rule: $ruleId"
      case _ => throw new Exception(s"Clean Failed for rule: $ruleId")
    }
  }

  // TODO This is doing far too much
  protected def moveJarToStage(ruleId: String, version: String): Future[LogEntry] = {
    val statusLog = logPartialStatus(Some(s"RULE: $ruleId"), PACKAGE)
    val errorLog = logPartialError(Some(s"RULE: $ruleId"), PACKAGE)

    val jar = executor.rule.jarFile(ruleId, version)
    val stageJar = executor.rule.jarStageFile(ruleId, version)
    stageJar.toJava.getParentFile.mkdirs()

    (for {
      _ <- changeStatus(ruleId, ARTIFACT_START)
      _ <- statusLog(INFO, s"Starting move Rule Jar from $jar to $stageJar")
      _ = jar copyTo stageJar
      logEntry <- handleRuleSuccess(ruleId, ARTIFACT_DONE, s"Moved Rule Jar from $jar to $stageJar")(statusLog)
      _ <- statusLog(INFO, logEntry.msg)
      qs <- cleanProject(ruleId, executor.rule.localFile(ruleId))
      _ <- statusLog(INFO, qs)
      _ = deleteSource(ruleId, version)
      ps <- whenBusy(ruleId, gitPullCommitAndPush(ruleId, version))
      _ <- statusLog(INFO, ps.msg)
      us <- whenBusy(ruleId, uploadJarToGoogle(ruleId, version))
    } yield us) recoverWith {
      case f: Throwable =>
        handleRuleFailure(ruleId, ARTIFACT_FAILED, f)(statusLog)(errorLog)
    }
  }
}