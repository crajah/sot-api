package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import cats.Monad
import io.finch._
import io.finch.circe._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import spray.json._
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.actions.VersionActions
import parallelai.sot.api.config._
import parallelai.sot.api.model.{Version, VersionActive, VersionToken}
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.{Result, ResultOps}
import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext

class VersionEndpoints(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with VersionActions with DefaultJsonProtocol {
  this: DatastoreConfig =>

  //implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  lazy val registerVersion = new RegisterVersionImpl

  val versionPath: Endpoint[HNil] = api.path :: "version"

  lazy val versionEndpoints = register :+: postVersion :+: versions :+: refreshVersion :+: deleteVersion :+: activeVersion :+: allActiveVersions

  lazy val register: Endpoint[Result[RegisteredVersion]] = {
    post(versionPath :: "register" :: jsonBody[Encrypted[VersionToken]]) { versionToken: Encrypted[VersionToken] =>
      registerVersion(versionToken).toTFuture

      /*val versionToken = versionTokenEncrypted.decrypt
      println(versionToken)

      Result(versionTokenEncrypted, Status.Ok).toTFuture*/
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
  def apply()(implicit sb: SttpBackend[Future, Nothing]) =
    (new VersionEndpoints with DatastoreConfig).versionEndpoints
}

abstract class RegisterVersion[F[_]: Monad] {
  def apply(versionToken: Encrypted[VersionToken]): F[Result[RegisteredVersion]]
}

case class RegisteredVersion()

object RegisteredVersion {
  implicit val encoder: Encoder[RegisteredVersion] = deriveEncoder[RegisteredVersion]

  implicit val decoder: Decoder[RegisteredVersion] = deriveDecoder[RegisteredVersion]
}

class RegisterVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends RegisterVersion[Future] with LicenceEndpointOps with ResultOps {
  def apply(versionToken: Encrypted[VersionToken]): Future[Result[RegisteredVersion]] =
    Future successful Result(RegisteredVersion(), Status.Ok)
}