package parallelai.sot.api.entities

import shapeless.datatype.datastore.DatastoreType._
import shapeless.datatype.datastore._
import spray.json._
import parallelai.common.secure.{ AES, CryptoMechanic, CryptoResult }
import parallelai.sot.api.config.secret

// TODO - To use
case class EncryptedString(value: String)

object EncryptedString {
  implicit object EncryptedStringFormat extends JsonFormat[EncryptedString] {
    def write(obj: EncryptedString) = JsString(obj.value)

    def read(json: JsValue): EncryptedString = json match {
      case JsString(s) => EncryptedString(s)
      case _ => throw new UnsupportedOperationException("Formats can only be a String")
    }
  }

  implicit val entityMappableType: BaseDatastoreMappableType[EncryptedString] = at[EncryptedString](fromEntity, toEntity)

  private val datastoreType = DatastoreType[CryptoResult[Array[Byte]]]

  private val crypto = new CryptoMechanic(AES, secret.getBytes())

  private def fromEntity(v: com.google.datastore.v1.Value): EncryptedString = {
    val dsEntity = v.getEntityValue // get the DataStore Entity object
    val cryptoResultObject = datastoreType.fromEntity(dsEntity) // Convert it to a Cryptoresult object

    cryptoResultObject match {
      case None =>
        new EncryptedString("NO CREDENTIALS FOUND IN DB")

      case Some(cres) =>
        val payload = cres.payload
        val params = cres.params

        val clearByteResult = crypto.decrypt[Array[Byte], Array[Byte]](payload, params)

        new EncryptedString(clearByteResult.payload.map(_.toChar).mkString)
    }
  }

  private def toEntity(encryptedString: EncryptedString): com.google.datastore.v1.Value = {
    val clearBytes = encryptedString.value.getBytes // get the bytes in the clear

    val cryptoResult = crypto.encrypt[Array[Byte], Array[Byte]](clearBytes) // Encrypt them.

    val ent = datastoreType.toEntityBuilder(cryptoResult).build() // Build the Datastore Entity

    com.google.datastore.v1.Value.newBuilder().setEntityValue(ent).build() // Return the value object
  }
}