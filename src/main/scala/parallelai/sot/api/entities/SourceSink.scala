package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._
import org.joda.time.Instant
import org.joda.time.Instant.now
import parallelai.common.persist.Identity

@deprecated(message = "I believe this is no longer used", since = "2nd March 2018")
case class SourceSink(id: String, `type`: SourceSinkType, name: String, schemaId: String, tapId: String, timestamp: Option[Instant] = Option(now)) extends TimedBaseEntity

object SourceSink {
  implicit val sourceSinkIdentity: Identity[SourceSink] = Identity[SourceSink](_.id)

  implicit val rootJsonFormat: RootJsonFormat[SourceSink] = jsonFormat6(SourceSink.apply)
}