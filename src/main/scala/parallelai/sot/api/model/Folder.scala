package parallelai.sot.api.model

import spray.json.DefaultJsonProtocol._
import spray.json._
import org.joda.time.Instant
import org.joda.time.Instant.now
import parallelai.common.persist.Identity

case class Folder(
  id: String,
  name: String,
  parent: String,
  schemas: List[String],
  sources: List[String],
  steps: List[String],
  taps: List[String],
  dags: List[String],
  timestamp: Option[Instant] = Some(now)) extends TimedBaseEntity

object Folder {
  implicit val folderIdentity: Identity[Folder] = Identity[Folder](_.id)

  implicit val rootJsonFormat: RootJsonFormat[Folder] = jsonFormat9(Folder.apply)
}