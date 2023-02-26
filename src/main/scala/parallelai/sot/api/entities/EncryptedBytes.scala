package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import parallelai.common.secure.CryptoMechanic

case class EncryptedBytes private (value: Array[Byte])

object EncryptedBytes {
  implicit val rootJsonFormat: RootJsonFormat[EncryptedBytes] = jsonFormat1(EncryptedBytes.apply)

  def apply[T: AsArray](value: T)(implicit crypto: CryptoMechanic) =
    new EncryptedBytes(crypto.encrypt(AsArray[T].apply(value)).payload.repr)
}