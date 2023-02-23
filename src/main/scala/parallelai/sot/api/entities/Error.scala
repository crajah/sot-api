package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._

// TODO - There is an io.finch.Error related to endpoint errors... could we use that instead?
case class Error(message: String)

object Error {
  implicit val rootJsonFormat: RootJsonFormat[Error] = jsonFormat(Error.apply, "error-message")
}