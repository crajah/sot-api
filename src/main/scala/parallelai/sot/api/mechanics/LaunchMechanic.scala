package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scala.sys.process.Process
import cats.data.EitherT
import cats.implicits._
import shapeless.datatype.datastore._
import org.slf4j.event.Level.INFO
import parallelai.sot.api.config.executor
import parallelai.sot.api.model._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.endpoints.Error

trait LaunchMechanic extends StatusMechanic with DatastoreMappableType {
  this: DatastoreConfig =>

  lazy val envDAO: ApiDatastore[Environment] = datastore[Environment]

  def launch(ruleLcm: RuleLcm): Future[Error Either LogEntry] = blocking {
    val ruleId = ruleLcm.id
    val ruleJarFile = executor.rule.jarLaunchFile(ruleId)

    val statusLog = logPartialStatus(Some(s"Rule $ruleId"), LAUNCH)
    val errorLog = logPartialError(Some(s"Rule $ruleId"), LAUNCH)

    def processResult(ls: LogEntry, processLogging: ProcessLogging, exitCode: Int) = {
      processLogging.flush()

      if (exitCode != 0 || processLogging.isError) EitherT.left[LogEntry](Error(s"Launch failed for rule $ruleId with error process code $exitCode").pure[Future])
      else EitherT.liftF[Future, Error, LogEntry](handleRuleSuccess(ruleId, LAUNCH_DONE, s"Rule $ruleId launch activated (${ls.msg}) - Please query status for details")(statusLog))
    }

    val result: EitherT[Future, Error, LogEntry] = for {
      _ <- EitherT liftF changeStatus(ruleId, LAUNCH_START)
      _ <- EitherT liftF statusLog(INFO, s"Rule $ruleId file $ruleJarFile launch started")
      env <- EitherT.fromOptionF(envDAO findOneById ruleLcm.envId, Error(s"Cannot launch rule $ruleId because of requested non existing environment ${ruleLcm.id}"))
      command = s"java -cp $ruleJarFile ${executor.launch.className} ${env.launchOpts} --jobName=$ruleId ${executor.launch.opts}"
      ls <- EitherT liftF statusLog(INFO, s"Rule $ruleId launching with command: $command")
      processLogging = new ProcessLogging(ruleId)
      exitCode = Process(command, ruleJarFile.toJava.getParentFile.getAbsoluteFile).!(processLogging)
      logEntry <- processResult(ls, processLogging, exitCode)
    } yield logEntry

    result.value.flatMap {
      case Left(e) =>
        handleRuleFailure(ruleId, DOWNLOAD_FAILED, new Exception(e.message))(statusLog)(errorLog).map(_ => Left(e))

      case Right(logEntry) =>
        Right(logEntry).pure[Future]
    }
  }
}