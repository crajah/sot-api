package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import io.finch.sprayjson._
import io.finch.syntax._
import io.finch.{Errors => _, _}
import shapeless.HNil
import spray.json._
import com.softwaremill.sttp.SttpBackend
import parallelai.common.secure._
import parallelai.sot.api.actions.VersionActions
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.Result
import parallelai.sot.api.http.service.RegisterVersionImpl
import parallelai.sot.api.model.{RegisteredVersion, Version, VersionActive}
import parallelai.sot.api.services.{LicenceService, VersionService}

class VersionEndpoints(versionService: VersionService, licenceService: LicenceService)(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with VersionActions with DefaultJsonProtocol {
  this: DatastoreConfig =>

  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  lazy val registerVersion = new RegisterVersionImpl(versionService, licenceService)

  val versionPath: Endpoint[HNil] = api.path :: "version"

  lazy val versionEndpoints = register :+: postVersion :+: versions :+: refreshVersion :+: deleteVersion :+: activeVersion :+: allActiveVersions

  lazy val register: Endpoint[Result[Encrypted[RegisteredVersion]]] = {
    import io.finch.circe._

    post(versionPath :: "register" :: jsonBody[Encrypted[Version]]) { version: Encrypted[Version] =>
      registerVersion(version).toTFuture
    }
  }

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
  def apply(versionService: VersionService, licenceService: LicenceService)(implicit sb: SttpBackend[Future, Nothing]) =
    (new VersionEndpoints(versionService, licenceService) with DatastoreConfig).versionEndpoints
}