package parallelai.sot.api.model

import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.common.persist.Identity

// TODO folder as String should be a (better) File
// TODO - Move
case class GitVersion(id: String, folder: String, isActive: Boolean)

object GitVersion {
  implicit val gitVersionIdentity: Identity[GitVersion] = Identity[GitVersion](_.id)

  implicit val rootJsonFormat: RootJsonFormat[GitVersion] = jsonFormat3(GitVersion.apply)
}