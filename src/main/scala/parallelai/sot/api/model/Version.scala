package parallelai.sot.api.model

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.lenses.JsonLenses._
import parallelai.common.persist.Identity

case class Version(value: String)

object Version {
  implicit val rootJsonFormat: RootJsonFormat[Version] = new RootJsonFormat[Version] {
    def write(version: Version): JsValue = JsObject("version" -> JsString(version.value))

    def read(json: JsValue): Version = Version(json.extract[String]("version"))
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