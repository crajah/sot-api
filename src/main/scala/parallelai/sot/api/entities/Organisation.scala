package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._
import org.apache.commons.lang.SerializationUtils._
import parallelai.common.secure.model.ToBytes

case class Organisation(id: String, code: String, email: String)

object Organisation {
  implicit val organisationToBytes: ToBytes[Organisation] =
    (organisation: Organisation) => serialize(organisation)

  implicit val rootJsonFormat: RootJsonFormat[Organisation] =
    jsonFormat3(Organisation.apply)
}