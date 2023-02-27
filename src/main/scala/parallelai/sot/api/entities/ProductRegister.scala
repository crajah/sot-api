package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.common.secure.model.EncryptedBytes

case class ProductRegister(organisation: Organisation, productToken: EncryptedBytes, dhkeClientPublicKey: Option[EncryptedBytes] = None)

object ProductRegister {
  implicit val rootJsonFormat: RootJsonFormat[ProductRegister] =
    jsonFormat3(ProductRegister.apply)
}