package parallelai.sot.api.model

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto._
import com.github.nscala_time.time.Imports._
import parallelai.common.secure.{FromBytes, ToBytes}
import org.apache.commons.lang3.SerializationUtils.{deserialize, serialize}

case class RegisteredVersion(uri: String, token: Token, expiry: DateTime)

object RegisteredVersion {
  implicit val toBytes: ToBytes[RegisteredVersion] =
    (registeredVersion: RegisteredVersion) => serialize(registeredVersion)

  implicit val fromBytes: FromBytes[RegisteredVersion] =
    (a: Array[Byte]) => deserialize[RegisteredVersion](a)

  implicit val dateTimeEncoder: Encoder[DateTime] =
    (dateTime: DateTime) => Json.fromString(dateTime.toString)

  implicit val dateTimeDecoder: Decoder[DateTime] =
    (c: HCursor) => c.last.as[String].map(DateTime.parse)

  implicit val encoder: Encoder[RegisteredVersion] = deriveEncoder

  implicit val decoder: Decoder[RegisteredVersion] = deriveDecoder
}