package parallelai.sot.api.entities

import io.circe.{ Decoder, Encoder }
import spray.json.DefaultJsonProtocol._
import spray.json._
import parallelai.common.secure.model.EncryptedBytes

case class ProductRegister(organisation: Organisation, productToken: EncryptedBytes, dhkeClientPublicKey: Option[EncryptedBytes] = None)

object ProductRegister {
  implicit val rootJsonFormat: RootJsonFormat[ProductRegister] =
    jsonFormat3(ProductRegister.apply)

  implicit val encoder: Encoder[ProductRegister] =
    Encoder.forProduct3("organisation", "productToken", "dhkeClientPublicKey")(p => (p.organisation, p.productToken, p.dhkeClientPublicKey))

  implicit val decoder: Decoder[ProductRegister] =
    Decoder.forProduct3("organisation", "productToken", "dhkeClientPublicKey")(ProductRegister.apply)
}