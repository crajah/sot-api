package parallelai.sot.api.mechanics

import java.io.InputStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files.File
import org.slf4j.event.Level.INFO
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.{ Bucket, StorageOptions }
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{ Response, ResponseException }
import parallelai.sot.api.config.executor
import parallelai.sot.api.entities._
import parallelai.sot.api.file.FileOps
import parallelai.sot.api.gcp.datastore.DatastoreConfig

trait GoogleStorageMechanic extends StatusMechanic with FileOps {
  this: DatastoreConfig =>

  def downloadJarToGoogle(ruleId: String): Future[LogEntry] = {
    val statusLog = logPartialStatus(Some(s"RULE: $ruleId"), DOWNLOAD)
    val errorLog = logPartialError(Some(s"RULE: $ruleId"), DOWNLOAD)

    (for {
      _ <- changeStatus(ruleId, DOWNLOAD_START)
      _ <- statusLog(INFO, s"Rule $ruleId file ${jarName(ruleId)} download started to ${jarName(ruleId)}")
      bucket <- googleBucket()
      up <- downloadFile(ruleId, bucket)
      result <- handleRuleSuccess(ruleId, DOWNLOAD_DONE, up)(statusLog)
    } yield result) recoverWith {
      case t: Throwable =>
        handleRuleFailure(ruleId, DOWNLOAD_FAILED, t)(statusLog)(errorLog).flatMap { e =>
          Future failed ResponseException(Response(Error(e.msg), Status.InternalServerError))
        }
    }
  }

  protected def downloadFile(ruleId: String, bucket: Bucket): Future[String] = Future {
    val ruleJarFile: File = executor.rule.jarLaunchFile(ruleId)
    ruleJarFile.parent.toJava.mkdirs()

    bucket.get(jarName(ruleId)).downloadTo(ruleJarFile.path)

    s"Rule $ruleId file ${jarName(ruleId)} download is complete to ${jarName(ruleId)}"
  }

  protected def uploadJarToGoogle(ruleId: String, version: String): Future[LogEntry] = {
    val statusLog = logPartialStatus(Some(s"RULE: $ruleId"), UPLOAD)
    val errorLog = logPartialError(Some(s"RULE: $ruleId"), UPLOAD)

    val ruleJarFile = executor.rule.jarStageFile(ruleId, version)

    def uploadFile(bucket: Bucket): Future[String] = Future {
      ruleJarFile.fileInputStream.apply { fin =>
        bucket.create(jarName(ruleId), fin)
        s"Rule $ruleId file ${jarName(ruleId)} upload is complete to bucket ${bucket.toString} as ${jarName(ruleId)}"
      }
    }

    (for {
      _ <- changeStatus(ruleId, UPLOAD_START)
      _ <- statusLog(INFO, s"Rule $ruleId file ${jarName(ruleId)} upload started")
      bkt <- googleBucket()
      up <- uploadFile(bkt)
      _ <- handleRuleSuccess(ruleId, UPLOAD_DONE, up)(statusLog)
      _ = ruleJarFile.parent delete false
      ds <- statusLog(INFO, s"Rule $ruleId deleted staged file ${jarName(ruleId)}")
      _ <- setRuleNotBusy(ruleId)
    } yield ds).recoverWith {
      case f: Throwable =>
        handleRuleFailure(ruleId, UPLOAD_FAILED, f)(statusLog)(errorLog)
    }
  }

  def googleBucket(credentialStream: Option[InputStream] = None): Future[Bucket] = Future {
    val storage = credentialStream match {
      case None =>
        StorageOptions.getDefaultInstance.getService

      case Some(is) =>
        val creds = GoogleCredentials.fromStream(is)
        StorageOptions.newBuilder().setCredentials(creds).build().getService
    }

    storage.get(executor.google.bucket)
  }
}