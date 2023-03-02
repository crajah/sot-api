package parallelai.sot.api.entities

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.security.KeyPair
import java.util.Base64
import io.circe.{Decoder, Encoder}
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.AdditionalFormats
import parallelai.common.secure.Encrypted
import parallelai.common.secure.diffiehellman.ClientPublicKey
import boopickle.Default._
import parallelai.sot.api.json.JsonLens._


case class ProductRegister(organisation: Organisation, productToken: Encrypted, clientPublicKey: Option[ClientPublicKey] = None)

object ProductRegister extends DefaultJsonProtocol {
  implicit val keyPairJsonFormat = new RootJsonFormat[KeyPair] {
    def read(json: JsValue): KeyPair = json match {
      case JsString(j) =>
        val bi: ByteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder.decode(j))
        val oi: ObjectInputStream = new ObjectInputStream(bi)
        val obj = oi.readObject()

        oi.close()
        bi.close()

        obj.asInstanceOf[KeyPair]

      case _ =>
        deserializationError("KeyPair reading error")
    }

    def write(keyPair: KeyPair): JsValue = {
      val b: ByteArrayOutputStream = new ByteArrayOutputStream()
      val o: ObjectOutputStream =  new ObjectOutputStream(b)
      o.writeObject(keyPair)
      val arr = b.toByteArray

      o.close()
      b.close()
      new JsString(Base64.getEncoder.encodeToString(arr))
    }
  }

  /*implicit val optionClientPublicKeyRootJsonFormat = new RootJsonFormat[Option[ClientPublicKey]] {
    def read(json: JsValue): Option[ClientPublicKey] = json.asJsObject.getFields("value", "keyPair") match {
      case Seq(JsString(value), JsString(keyPair)) => Option(ClientPublicKey(value.getBytes, deserialize(keyPair.getBytes).asInstanceOf[KeyPair]))
      case _ => None
    }

    def write(clientPublicKey: Option[ClientPublicKey]): JsValue = clientPublicKey match {
      case None => JsObject()
      case Some(c) => JsObject("value" -> JsString(new String(c.value)), "keyPair" -> JsString(new String(serialize(c.keyPair))))
    }
  }*/

  implicit val clientPublicKeyRootJsonFormat = jsonFormat2(ClientPublicKey.apply)
  /*implicit val clientPublicKeyRootJsonFormat = new RootJsonFormat[ClientPublicKey] {
    def read(json: JsValue): ClientPublicKey = json.asJsObject.getFields("value", "keyPair") match {
      case Seq(JsString(value), JsString(keyPair)) =>
        ClientPublicKey(value.getBytes, Unpickle[KeyPair].fromBytes(ByteBuffer.wrap(keyPair.getBytes)))

      case _ =>
        deserializationError("ClientPublicKey reading error")
    }

    def write(clientPublicKey: ClientPublicKey): JsValue =
      JsObject("value" -> JsString(new String(clientPublicKey.value)), "keyPair" -> JsString(Pickle.intoBytes(clientPublicKey.keyPair).toString))
  }*/


  implicit val rootJsonFormat: RootJsonFormat[ProductRegister] =
    jsonFormat3(ProductRegister.apply)

  /*implicit val encoder: Encoder[ProductRegister] =
    Encoder.forProduct3("organisation", "productToken", "clientPublicKey")(p => (p.organisation, p.productToken, p.clientPublicKey))

  implicit val decoder: Decoder[ProductRegister] =
    Decoder.forProduct3("organisation", "productToken", "clientPublicKey")(ProductRegister.apply)*/
}