package parallelai.sot.api.model

import java.util.Base64
import io.circe.Decoder.Result
import io.circe._
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.lenses.JsonLenses._
import parallelai.common.persist.Identity
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.generic.semiauto._
import com.github.nscala_time.time
import com.github.nscala_time.time.Imports
import org.apache.commons.lang3.SerializationUtils.{deserialize, serialize}
import parallelai.common.secure.{FromBytes, ToBytes}
import io.finch.circe._

// TODO - Extract into "sot-api-common"
import com.github.nscala_time.time.Imports._
import cats.syntax.either._

case class VersionToken(licenseId: String, organisationCode: String, version: Version, expiry: DateTime = DateTime.nextDay)

object VersionToken {
  implicit val toBytes: ToBytes[VersionToken] =
    (versionToken: VersionToken) => serialize(versionToken)

  implicit val fromBytes: FromBytes[VersionToken] =
    (a: Array[Byte]) => deserialize[VersionToken](a)

  implicit val dataTimeEncoder: Encoder[DateTime] =
    (dateTime: DateTime) => Json.fromString(dateTime.toString)

  implicit val dateTimeDecoder: Decoder[DateTime] =
    (c: HCursor) => c.last.as[String].map(DateTime.parse)

  implicit val encoder: Encoder[VersionToken] = deriveEncoder[VersionToken]

  /*implicit val encoder: Encoder[VersionToken] = (versionToken: VersionToken) =>
    Json.obj(
      "licenceId" -> Json.fromString(versionToken.licenseId),
      "organisationCode" -> Json.fromString(versionToken.organisationCode),
      "version" -> Json.fromString(versionToken.version.value),
      "expiry" -> versionToken.expiry.asJson
    )*/

  implicit val decoder: Decoder[VersionToken] = deriveDecoder[VersionToken]

  /*implicit val decoder: Decoder[VersionToken] = (c: HCursor) => for {
    licenceId <- c.downField("licenceId").as[String]
    organisationCode <- c.downField("organisationCode").as[String]
    version <- c.downField("version").as[Version]
    expiry <- c.downField("expiry").as[DateTime]
  } yield VersionToken(licenceId, organisationCode, version, expiry)*/
}

case class Version(value: String)

object Version {
  implicit val rootJsonFormat: RootJsonFormat[Version] = new RootJsonFormat[Version] {
    def write(version: Version): JsValue = JsObject("version" -> JsString(version.value))

    def read(json: JsValue): Version = Version(json.extract[String]("version"))

    implicit val encoder: Encoder[Version] = deriveEncoder[Version]
    implicit val decoder: Decoder[Version] = deriveDecoder[Version]
  }
}

case class VersionActive(version: String, active: Boolean)

object VersionActive {
  implicit val rootJsonFormat: RootJsonFormat[VersionActive] = jsonFormat2(VersionActive.apply)
}

case class Versions(versions: Seq[VersionActive])

object Versions {
  implicit val rootJsonFormat: RootJsonFormat[Versions] = jsonFormat1(Versions.apply)
}

// TODO folder as String should be a (better) File
case class GitVersion(id: String, folder: String, isActive: Boolean)

object GitVersion {
  implicit val gitVersionIdentity: Identity[GitVersion] = Identity[GitVersion](_.id)

  implicit val rootJsonFormat: RootJsonFormat[GitVersion] = jsonFormat3(GitVersion.apply)
}