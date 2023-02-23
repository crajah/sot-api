package parallelai.sot.api.actions

import spray.json._
import com.twitter.finagle.http.Status
import parallelai.sot.api.json.JsonLens._

case class Response(content: JsValue, status: Status)

object Response extends DefaultJsonProtocol {
  implicit val statusJsonWriter: JsonWriter[Status] =
    (status: Status) => JsObject(
      "code" -> JsNumber(status.code),
      "reason" -> JsString(status.reason))

  implicit val responseWriter: JsonWriter[Response] =
    (response: Response) => JsObject("content" -> response.content) << ("http-status", response.status.toJson)

  def apply[J: JsonWriter](j: J, status: Status = Status.Ok) =
    new Response(implicitly[JsonWriter[J]].write(j), status)
}