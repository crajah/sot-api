package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process.ProcessLogger
import scala.util.{Failure, Success, Try}
import better.files._
import cats.data.EitherT
import cats.implicits._
import grizzled.slf4j.Logging
import shapeless.datatype.datastore.DatastoreType.at
import shapeless.datatype.datastore._
import spray.json.DefaultJsonProtocol._
import spray.json._
import org.slf4j.event.Level
import org.slf4j.event.Level._
import com.twitter.finagle.http.Status
import parallelai.sot.api.disjunction.EitherOps
import parallelai.sot.api.endpoints.{Error, Response, ResponseException}
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.DatastoreConfig

// TODO ???
trait Enum[A] {
  trait Value { self: A =>
    _values :+= this
  }

  // TODO - A var? Can we get rid of this?
  private var _values = List.empty[A]

  def values: List[A] = _values

  // TODO - toString on a parametric instance?
  def fromName(name: String): Option[A] =
    values.filter(_.toString.equalsIgnoreCase(name)) match {
      case x :: Nil => Some(x)
      case _ => None
    }
}

object StatusType extends Enum[StatusType] {
  implicit val rootJsonFormat: RootJsonFormat[StatusType] = new RootJsonFormat[StatusType] {
    def write(s: StatusType) = JsString(s.toString)

    def read(value: JsValue): StatusType = fromName(value.convertTo[String]) getOrElse INVALID
  }

  implicit val entityMappableType: BaseDatastoreMappableType[StatusType] =
    at[StatusType](toE, fromE)

  private def toE(value: com.google.datastore.v1.Value): StatusType =
    fromName(value.getStringValue) getOrElse INVALID // TODO

  private def fromE(statusType: StatusType): com.google.datastore.v1.Value =
    com.google.datastore.v1.Value.newBuilder().setStringValue(statusType.toString).build()
}

sealed trait StatusType extends StatusType.Value

case object START extends StatusType
case object VALIDATE_DONE extends StatusType
case object CODE_DONE extends StatusType
case object CONFIG_DONE extends StatusType
case object BUILD_START extends StatusType
case object BUILD_DONE extends StatusType
case object BUILD_FAILED extends StatusType
case object GIT_START extends StatusType
case object GIT_DONE extends StatusType
case object GIT_FAILED extends StatusType
case object ARTIFACT_START extends StatusType
case object ARTIFACT_DONE extends StatusType
case object ARTIFACT_FAILED extends StatusType
case object UPLOAD_START extends StatusType
case object UPLOAD_DONE extends StatusType
case object UPLOAD_FAILED extends StatusType
case object DOWNLOAD_START extends StatusType
case object DOWNLOAD_DONE extends StatusType
case object DOWNLOAD_FAILED extends StatusType
case object LAUNCH_START extends StatusType
case object LAUNCH_DONE extends StatusType
case object LAUNCH_FAILED extends StatusType
case object INVALID extends StatusType

object Stage extends Enum[Stage]

sealed trait Stage extends Stage.Value

case object VERSION extends Stage
case object GIT extends Stage
case object CONFIG extends Stage
case object BUILD extends Stage
case object PACKAGE extends Stage
case object UPLOAD extends Stage
case object DOWNLOAD extends Stage
case object LAUNCH extends Stage
case object BUSY extends Stage
case object RESUME extends Stage

trait StatusMechanic extends DatastoreMappableType with Logging with EitherOps {
  this: DatastoreConfig =>

  lazy val ruleStatusDAO: ApiDatastore[RuleStatus] = datastore[RuleStatus]

  lazy val ruleBusyDAO: ApiDatastore[RuleBusy] = datastore[RuleBusy]

  lazy val ruleInfoDAO: ApiDatastore[RuleInfo] = datastore[RuleInfo]

  lazy val logEntryDAO: ApiDatastore[LogEntry] = datastore[LogEntry]

  def changeStatus(ruleId: String, status: StatusType, statusDescription: Option[String] = None, envId: Option[String] = None): Future[String] =
    ruleStatusDAO put RuleStatus(ruleId, status, statusDescription, envId) map { ruleStatus =>
      s"Rule ${ruleStatus.id} Status changed to ${ruleStatus.status}"
    }

  def setRuleBusy(ruleId: String): Future[RuleBusy] =
    setRuleBusyState(ruleId, busy = true)

  def setRuleBusyState(ruleId: String, busy: Boolean): Future[RuleBusy] =
    for {
      ra <- ruleBusyDAO put RuleBusy(ruleId, busy)
      _ <- logStatusFulfil(Some(s"RULE: $ruleId"), BUSY, INFO, Right(s"Rule $ruleId busy state switched ${if (busy) "on" else "off"}"))
    } yield ra

  def logStatus(ident: Option[String], stage: Stage, level: Level, msg: String): Future[LogEntry] =
    logStatusFulfil(ident, stage, level, Right(msg))

  def logError(ident: Option[String], stage: Stage, level: Level, err: Throwable): Future[LogEntry] =
    logStatusFulfil(ident, stage, level, Left(err))

  def logPartialStatus(ident: Option[String], stage: Stage): (Level, String) => Future[LogEntry] =
    logStatus(ident, stage, _: Level, _: String)

  def logPartialError(ident: Option[String], stage: Stage): (Level, Throwable) => Future[LogEntry] =
    logError(ident, stage, _: Level, _: Throwable)

  def handleRuleTry(ruleId: String, statusType: (StatusType, StatusType), t: Try[String])(statusLogger: (Level, String) => Future[LogEntry])(errorLogger: (Level, Throwable) => Future[LogEntry]): Future[LogEntry] =
    t match {
      case Success(s) => handleRuleSuccess(ruleId, statusType._1, s)(statusLogger)
      case Failure(f) => handleRuleFailure(ruleId, statusType._2, f)(statusLogger)(errorLogger)
    }

  def handleRuleSuccess(ruleId: String, statusType: StatusType, s: String)(statusLogger: (Level, String) => Future[LogEntry]): Future[LogEntry] =
    for {
      _ <- statusLogger(INFO, s)
      ms <- changeStatus(ruleId, statusType)
      mf <- statusLogger(INFO, ms)
    } yield mf

  def handleRuleFailure(ruleId: String, statusType: StatusType, t: Throwable)(statusLogger: (Level, String) => Future[LogEntry])(errorLogger: (Level, Throwable) => Future[LogEntry]): Future[LogEntry] =
    for {
      _ <- errorLogger(ERROR, t)
      ms <- changeStatus(ruleId, statusType, Option(t.getMessage))
      _ <- statusLogger(ERROR, ms)
      ri <- setRuleNotBusy(ruleId)
      mf <- statusLogger(ERROR, ri.toJson.compactPrint)
    } yield mf

  private def logStatusFulfil(ident: Option[String], stage: Stage, level: Level, msg: Either[Throwable, String]): Future[LogEntry] = {
    def getLogger: (String => Unit) = level match {
      case DEBUG => debug(_: String)
      case INFO => info(_: String)
      case ERROR => error(_: String)
      case WARN => warn(_: String)
      case TRACE => trace(_: String)
      case _ => debug(_: String)
    }

    def getLoggerWithThrowable: (String, Throwable) => Unit = level match {
      case DEBUG => debug(_: String, _: Throwable)
      case INFO => info(_: String, _: Throwable)
      case ERROR => error(_: String, _: Throwable)
      case WARN => warn(_: String, _: Throwable)
      case TRACE => trace(_: String, _: Throwable)
      case _ => debug(_: String, _: Throwable)
    }

    def addLogEntry(): Future[LogEntry] = {
      val logEntry = LogEntry(stage = stage.toString, level = level.toString, ident = ident, msg = "", trace = None)

      logEntryDAO.put(
        msg match {
          case Left(t) =>
            /*
            TODO - The following cannot be used with Datastore because of a possible
            com.google.cloud.datastore.DatastoreException: The value of property "trace" is longer than 1500 bytes
            See https://cloud.google.com/appengine/docs/standard/python/datastore/typesandpropertyclasses
            */
            // logEntry.copy(msg = t.getMessage, trace = Some(t.getStackTrace.map(s => s.toString).mkString("\n")))
            val trace = t.getStackTrace.map(s => s.toString).mkString("\n")
            logEntry.copy(msg = t.getMessage, trace = Some(if (trace.length > 1500) trace.substring(0, 1500) else trace))
          case Right(m) =>
            logEntry.copy(msg = m)
        })
    }

    msg match {
      case Left(t) =>
        getLoggerWithThrowable(t.getMessage, t)
        addLogEntry()

      case Right(m) =>
        getLogger(m)
        addLogEntry()
    }
  }

  def when[T](mustBeBusy: Boolean, ruleId: String, error: Error, f: => Future[T]): Future[T] = {
    val result = for {
      b <- EitherT(busy(ruleId))
      _ <- if (b == mustBeBusy) right[Future, Response, Boolean](b) else left[Future, Response, Boolean](Response(error, Status.BadRequest))
      result <- EitherT.liftF[Future, Response, T](f)
    } yield result

    result.value.flatMap {
      case Left(r) => Future failed ResponseException(r)
      case Right(t) => Future successful t
    }
  }

  def whenBusy[T](ruleId: String, f: => Future[T]): Future[T] =
    when[T](mustBeBusy = true, ruleId, Error(s"Rule $ruleId is not busy - Cannot proceed"), f)

  def whenNotBusy[T](ruleId: String, f: => Future[T]): Future[T] =
    when[T](mustBeBusy = false, ruleId, Error(s"Rule $ruleId is busy - Cannot proceed"), f)

  def busy[T](ruleId: String, f: => Future[T]): Future[T] =
    setRuleBusy(ruleId).flatMap(_ => f)

  protected def setRuleNotBusy(ruleId: String): Future[RuleBusy] =
    setRuleBusyState(ruleId, busy = false)

  protected def busy(ruleId: String): Future[Response Either Boolean] =
    ruleBusyDAO findOneById ruleId map {
      case None => Either left Response(Error(s"Non existing rule: $ruleId - Cannot proceed"), Status.NotFound)
      case Some(r) => Either right r.busy
    }

  protected def ruleBuildStatus(ruleId: String): Future[Option[RuleStatus]] = ruleStatusDAO findOneById ruleId

  protected def ruleInfo(ruleId: String): Future[Option[RuleInfo]] = ruleInfoDAO findOneById ruleId

  protected def setRuleInfo(ruleId: String, version: String, name: Option[String], description: Option[String], codeDirectory: File): Future[RuleInfo] =
    ruleInfoDAO put RuleInfo(ruleId, name, version, description, codeDirectory.pathAsString)

  class ProcessLogging(ruleId: String) extends ProcessLogger {
    val statusLog: (Level, String) => Future[LogEntry] =
      logPartialStatus(Some(s"RULE: $ruleId"), BUILD)

    val errorLog: (Level, Throwable) => Future[LogEntry] =
      logPartialError(Some(s"RULE: $ruleId"), BUILD)

    private val infoStringBuilder = new StringBuilder

    private val errorStringBuilder = new StringBuilder

    def isError: Boolean = errorStringBuilder.nonEmpty

    def flush(): Unit = {
      val errorMessage = errorStringBuilder.toString()

      if (errorMessage.nonEmpty) {
        error(errorMessage)

        statusLog(ERROR, errorMessage) foreach { logEntry =>
          errorStringBuilder.clear()
          logEntry
        }
      }

      if (infoStringBuilder.nonEmpty) {
        statusLog(INFO, infoStringBuilder.toString) foreach { logEntry =>
          infoStringBuilder.clear()
          logEntry
        }
      }
    }

    /**
     * Will be called with each line read from the process output stream.
     */
    override def out(s: => String): Unit = {
      infoStringBuilder ++= s"$s\n"

      if (infoStringBuilder.length > 1000) {
        statusLog(INFO, infoStringBuilder.toString())
        infoStringBuilder.clear
      }
    }

    /**
     * Will be called with each line read from the process error stream.
     */
    override def err(s: => String): Unit = {
      errorStringBuilder ++= s"$s\n"
      ()
    }

    /**
     * If a process is begun with one of these `ProcessBuilder` methods:
     *  {{{
     *    def !(log: ProcessLogger): Int
     *    def !<(log: ProcessLogger): Int
     *  }}}
     *  The run will be wrapped in a call to buffer.  This gives the logger
     *  an opportunity to set up and tear down buffering.  At present the
     *  library implementations of `ProcessLogger` simply execute the body
     *  unbuffered.
     */
    override def buffer[T](f: => T): T = f
  }
}