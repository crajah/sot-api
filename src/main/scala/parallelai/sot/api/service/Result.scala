package parallelai.sot.api.service

import io.circe._
import com.twitter.finagle.http.Status
import io.circe.syntax._

case class Result[T: Encoder: Decoder](value: Either[Errors, T], status: Status)

// TODO - Hateoas, such as links
object Result {
  implicit val statusEncoder: Encoder[Status] = (status: Status) =>
    Json.obj(
      "code" -> Json.fromInt(status.code),
      "reason" -> Json.fromString(status.reason)
    )

  implicit val statusDecoder: Decoder[Status] = (c: HCursor) =>
    for {
      code <- c.downField("code").as[Int]
    } yield Status(code)

  implicit def resultEncoder[T: Encoder]: Encoder[Result[T]] = (result: Result[T]) => {
    val json = result.value match {
      case Left(es) =>
        Json.obj("errors" -> Json.fromValues(es.messages.map(Json.fromString)))

      case Right(t) =>
        Json.obj("content" -> t.asJson)
    }

    Json.obj("status" -> result.status.asJson) deepMerge json
  }

  // TODO - Clean up this mess
  implicit def resultDecoder[T: Encoder: Decoder]: Decoder[Result[T]] = (c: HCursor) => {
    val content = for {
      content <- c.downField("content").as[T]
      status <- c.downField("status").as[Status]
    } yield Result[T](Right(content), status)

    content match {
      case r @ Right(_) => r
      case _ =>
        for {
          errors <- c.downField("errors").as[Seq[String]]
          status <- c.downField("status").as[Status]
        } yield Result[T](Left(Errors(errors: _*)), status)
    }
  }

  def apply[T: Encoder: Decoder](value: T, status: Status): Result[T] = Result(Right(value), status)

  def apply(errors: Errors, status: Status): Result[Errors] = Result[Errors](Left(errors), status)
}

case class Errors(messages: String*)

object Errors {
  implicit val errorsEncoder: Encoder[Errors] = (errors: Errors) =>
    Json fromValues errors.messages.map(Json.fromString)

  // TODO - Clean up this mess
  implicit val errorsDecoder: Decoder[Errors] = (c: HCursor) => {
    Right(c.top.get.asArray.fold(Errors()) { jsonErrors => Errors(jsonErrors.map(_.as[String].right.get): _*) })
  }
}