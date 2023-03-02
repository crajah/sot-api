package parallelai.sot.api.entities

import io.circe.{Decoder, Encoder}
import spray.json.DefaultJsonProtocol._
import spray.json._
import org.apache.commons.lang.SerializationUtils._
import parallelai.common.secure.ToBytes

case class Organisation(id: String, code: String, email: String)

object Organisation {
  implicit val organisationToBytes: ToBytes[Organisation] =
    (organisation: Organisation) => serialize(organisation)

  implicit val rootJsonFormat: RootJsonFormat[Organisation] =
    jsonFormat3(Organisation.apply)

  implicit val encoder: Encoder[Organisation] =
    Encoder.forProduct3("id", "code", "email")(o => (o.id, o.code, o.email))

  implicit val decoder: Decoder[Organisation] =
    Decoder.forProduct3("id", "code", "email")(Organisation.apply)
}