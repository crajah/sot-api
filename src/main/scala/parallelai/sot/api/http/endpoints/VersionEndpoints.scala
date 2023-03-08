package parallelai.sot.api.http.endpoints

import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import spray.json._
import parallelai.sot.api.actions.VersionActions
import parallelai.sot.api.config._
import parallelai.sot.api.model.{Version, VersionActive}
import parallelai.sot.api.gcp.datastore.DatastoreConfig

trait VersionEndpoints extends EndpointOps with VersionActions with DefaultJsonProtocol {
  this: DatastoreConfig =>

  val versionPath: Endpoint[HNil] = api.path :: "version"

  lazy val versionEndpoints = postVersion :+: versions :+: refreshVersion :+: deleteVersion :+: activeVersion :+: allActiveVersions

  lazy val postVersion: Endpoint[Response] =
    post(versionPath :: jsonBody[Version] :: paramOption[Boolean]("wait")) { (version: Version, wait: Option[Boolean]) =>
      postVersion(version, wait).toTFuture
    }

  lazy val versions: Endpoint[Response] =
    get(versionPath) { getVersions.toTFuture }

  lazy val refreshVersion: Endpoint[Response] =
    post(versionPath :: "refresh" :: jsonBody[Version] :: paramOption[Boolean]("wait")) { (version: Version, wait: Option[Boolean]) =>
      refreshVersion(version, wait).toTFuture
    }

  lazy val deleteVersion: Endpoint[Response] =
    post(versionPath :: "delete" :: jsonBody[Version]) { version: Version =>
      deleteVersion(version).toTFuture
    }

  lazy val activeVersion: Endpoint[Response] =
    post(versionPath :: "active" :: jsonBody[VersionActive]) { versionActive: VersionActive =>
      activeVersion(versionActive).toTFuture
    }

  lazy val allActiveVersions: Endpoint[Response] =
    get(versionPath :: "active") { getActiveVersions.toTFuture }
}

object VersionEndpoints {
  def apply() = (new VersionEndpoints with DatastoreConfig).versionEndpoints
}