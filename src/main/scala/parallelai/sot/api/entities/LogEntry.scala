package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.common.persist.Identity
import parallelai.common.persist.datastore.excludeFromIndexes

case class LogEntry(id: String = System.currentTimeMillis.toString, ident: Option[String], stage: String, level: String, @excludeFromIndexes msg: String, trace: Option[String] = None)

object LogEntry {
  implicit val logEntryIdentity: Identity[LogEntry] = Identity[LogEntry](_.id)

  implicit val rootJsonFormat: RootJsonFormat[LogEntry] = jsonFormat6(LogEntry.apply)
}