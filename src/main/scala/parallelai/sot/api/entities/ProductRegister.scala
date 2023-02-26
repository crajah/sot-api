package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._

case class ProductRegister(productToken: EncryptedBytes)

object ProductRegister {
  implicit val rootJsonFormat: RootJsonFormat[ProductRegister] = jsonFormat1(ProductRegister.apply)
}