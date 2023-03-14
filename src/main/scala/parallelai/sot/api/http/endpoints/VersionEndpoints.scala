package parallelai.sot.api.http.endpoints

import java.net.URI
import scala.concurrent.Future
import cats.Monad
import cats.implicits._
import io.finch.sprayjson._
import io.finch.syntax._
import io.finch.{Errors => _, _}
import shapeless.HNil
import spray.json._
import com.github.nscala_time.time.Imports._
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import parallelai.common.secure._
import parallelai.sot.api.actions.VersionActions
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.{Errors, Result, ResultOps}
import parallelai.sot.api.model.{RegisteredVersion, Token, Version, VersionActive}
import parallelai.sot.api.services.VersionService

class VersionEndpoints(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with VersionActions with DefaultJsonProtocol {
  this: DatastoreConfig =>

  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  lazy val registerVersion = new RegisterVersionImpl

  val versionPath: Endpoint[HNil] = api.path :: "version"

  lazy val versionEndpoints = register :+: postVersion :+: versions :+: refreshVersion :+: deleteVersion :+: activeVersion :+: allActiveVersions

  lazy val register: Endpoint[Result[Encrypted[RegisteredVersion]]] = {
    import io.finch.circe._

    //TODO this code is YUK!
    post(versionPath :: "register" :: jsonBody[Encrypted[Version]]) { version: Encrypted[Version] =>
      val decrypted: Version = Encrypted.decrypt(version)
      def isExpired(expiringDate: DateTime): Boolean = expiringDate.isBeforeNow

      if (isExpired(decrypted.expiry.getOrElse(new DateTime().minusDays(1)))) Result[Encrypted[RegisteredVersion]](Left(Errors("Version expired!")), Status.UnprocessableEntity).toTFuture else {
        versionService.versions += ((decrypted.token.get.code, decrypted.value) -> decrypted)

        registerVersion(version).toTFuture
      }
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
  def apply(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) =
    (new VersionEndpoints(versionService) with DatastoreConfig).versionEndpoints
}

abstract class RegisterVersion[F[_]: Monad] {
  def apply(versionToken: Encrypted[Version]): F[Result[Encrypted[RegisteredVersion]]]
}

class RegisterVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends RegisterVersion[Future] with LicenceEndpointOps with ResultOps {
  def apply(versionToken: Encrypted[Version]): Future[Result[Encrypted[RegisteredVersion]]] =
    Future successful Result(Encrypted(RegisteredVersion(new URI(""), Token("", "", ""), new DateTime()), Crypto(AES, secret.getBytes)), Status.Ok)
}