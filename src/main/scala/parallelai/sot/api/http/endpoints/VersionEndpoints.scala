package parallelai.sot.api.http.endpoints

import java.net.URI

import cats.Monad
import cats.implicits._
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import org.apache.commons.lang3.SerializationUtils.{deserialize, serialize}
import org.joda.time.DateTime
import parallelai.common.secure._
import parallelai.sot.api.actions.VersionActions
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.{Result, ResultOps}
import parallelai.sot.api.model.{RegisteredVersion, Token, Version, VersionActive}
import parallelai.sot.api.services.VersionService
import shapeless.HNil
import spray.json._

import scala.concurrent.Future

class VersionEndpoints(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with VersionActions with DefaultJsonProtocol {
  this: DatastoreConfig =>

  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  lazy val registerVersion = new RegisterVersionImpl

  val versionPath: Endpoint[HNil] = api.path :: "version"

  lazy val versionEndpoints = register :+: postVersion :+: versions :+: refreshVersion :+: deleteVersion :+: activeVersion :+: allActiveVersions

  lazy val register: Endpoint[Result[Encrypted[RegisteredVersion]]] = {
    import io.finch.circe._

    post(versionPath :: "register" :: jsonBody[Encrypted[Version]]) { version: Encrypted[Version] =>
      val decrypted: Version = Encrypted.decrypt(version)
      versionService.versions += ((decrypted.token.get.code, decrypted.value) -> decrypted)

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
  def apply(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) =
    (new VersionEndpoints(versionService) with DatastoreConfig).versionEndpoints
}

abstract class RegisterVersion[F[_]: Monad] {
  def apply(versionToken: Encrypted[Version]): F[Result[Encrypted[RegisteredVersion]]]
}

class RegisterVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends RegisterVersion[Future] with LicenceEndpointOps with ResultOps {
  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  def apply(versionToken: Encrypted[Version]): Future[Result[Encrypted[RegisteredVersion]]] =
    Future successful Result(Encrypted(RegisteredVersion(new URI(""), Token("", "", ""), new DateTime())), Status.Ok)
}