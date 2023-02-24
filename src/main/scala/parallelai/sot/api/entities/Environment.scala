package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._
import org.joda.time.Instant
import org.joda.time.Instant.now
import parallelai.common.persist.Identity

case class Environment(id: String, name: String, credentials: EncryptedString, launchOpts: String, projectId: String, timestamp: Option[Instant] = Option(now)) extends TimedBaseEntity

object Environment {
  implicit val environmentIdentity: Identity[Environment] = Identity[Environment](_.id)

  implicit val rootJsonFormat: RootJsonFormat[Environment] = jsonFormat6(Environment.apply)
}