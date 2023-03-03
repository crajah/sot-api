package parallelai.sot.api.actions

import scala.concurrent.{ExecutionContext, Future, blocking}
import cats.implicits._
import spray.json._
import com.twitter.finagle.http.Status
import parallelai.sot.api.concurrent.FileExcecutionContext
import parallelai.sot.api.config.executor
import parallelai.sot.api.endpoints.{Error, Response, ResponseException}
import parallelai.sot.api.entities.{GitVersion, Version, VersionActive, Versions}
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.mechanics.GitMechanic

trait VersionActions extends GitMechanic with DefaultJsonProtocol {
  this: DatastoreConfig =>

  private implicit val ec: ExecutionContext = FileExcecutionContext()

  protected def getVersions: Future[Response] = blocking {
    allVersions map { versions: List[GitVersion] =>
      val vers = versions map { gv: GitVersion =>
        VersionActive(gv.id, gv.isActive)
      }

      Response(Versions(vers))
    }
  }

  protected def getActiveVersions: Future[Response] =
    activeVersions map { versions: List[GitVersion] =>
      val vers = versions map { gv: GitVersion =>
        VersionActive(gv.id, gv.isActive)
      }

      Response(Versions(vers))
    }

  protected def refreshVersion(version: Version, wait: Option[Boolean]): Future[Response] = blocking {
    val gitLocalFolderForVersion = executor.git.localFile(version.value)

    if (directoryNotExists(gitLocalFolderForVersion)) {
      Future failed ResponseException(Response(Error(s"No git repository to reload at $gitLocalFolderForVersion"), Status.NotFound))
    } else {
      val refresh = refreshVersion(version.value).map(Response(_))

      wait match {
        case Some(true) => refresh
        case _ => Response(s"Reloading version ${version.value}", Status.Accepted).pure[Future]
      }
    }
  }

  /*
  val gitLocalFolderForVersion = executor.git.localFile(version)

    if (directoryNotExists(gitLocalFolderForVersion)) {
      Future failed ResponseException(Response(Error(s"No git repository to reload at $gitLocalFolderForVersion"), Status.NotFound))
   */

  protected def postVersion(version: Version, wait: Option[Boolean] = None): Future[Response] = {
    val gitVersion = addVersion(version.value)

    wait match {
      case Some(true) => gitVersion.map(Response(_))
      case _ => Response(s"Creating version ${version.value}", Status.Accepted).pure[Future]
    }
  }

  protected def deleteVersion(version: Version): Future[Response] =
    deleteVersion(version.value) map { _ =>
      Response("") // TODO
    }

  protected def activeVersion(versionActive: VersionActive): Future[Response] =
    changeVersionActiveStatus(versionActive.version, versionActive.active).map(Response(_))
}