package parallelai.sot.api.entities

import java.security.KeyPair
import java.util.Base64._
import io.circe
import io.circe.{Decoder, Encoder, HCursor, Json}
import spray.json._
import org.apache.commons.lang.SerializationUtils._
import parallelai.common.secure.Encrypted
import parallelai.common.secure.diffiehellman.ClientPublicKey

case class ProductRegister(organisation: Organisation, productToken: Encrypted, clientPublicKey: Option[ClientPublicKey] = None)

object ProductRegister extends DefaultJsonProtocol {
  implicit val keyPairJsonFormat: RootJsonFormat[KeyPair] = new RootJsonFormat[KeyPair] {
    def read(json: JsValue): KeyPair = json match {
      case JsString(j) => deserialize(getDecoder decode j).asInstanceOf[KeyPair]
      case _ => deserializationError("KeyPair reading error")
    }

    def write(keyPair: KeyPair): JsValue =
      new JsString(getEncoder encodeToString serialize(keyPair))
  }

  implicit val clientPublicKeyRootJsonFormat: RootJsonFormat[ClientPublicKey] =
    jsonFormat2(ClientPublicKey.apply)

  implicit val rootJsonFormat: RootJsonFormat[ProductRegister] =
    jsonFormat3(ProductRegister.apply)

  implicit val clientPublicKeyEncoder: Encoder[ClientPublicKey] = (clientPublicKey: ClientPublicKey) => Json.obj(
    ("value", circe.Json.fromString(getEncoder encodeToString serialize(clientPublicKey.value))),
    ("keyPair", circe.Json.fromString(getEncoder encodeToString serialize(clientPublicKey.keyPair)))
  )

  implicit val clientPublicKeyDecoder: Decoder[ClientPublicKey] = (hCursor: HCursor) => for {
    valueString <- hCursor.downField("value").as[String]
    keyPairString <- hCursor.downField("keyPair").as[String]
  } yield ClientPublicKey(getDecoder decode valueString, deserialize(getDecoder decode keyPairString).asInstanceOf[KeyPair])

  implicit val encoder: Encoder[ProductRegister] =
    Encoder.forProduct3("organisation", "productToken", "clientPublicKey")(p => (p.organisation, p.productToken, p.clientPublicKey))

  implicit val decoder: Decoder[ProductRegister] =
    Decoder.forProduct3("organisation", "productToken", "clientPublicKey")(ProductRegister.apply)
}