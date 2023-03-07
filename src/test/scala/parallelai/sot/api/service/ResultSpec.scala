package parallelai.sot.api.service

import io.circe.Json
import org.scalatest.{MustMatchers, WordSpec}
import com.twitter.finagle.http.Status
import io.circe.syntax._

class ResultSpec extends WordSpec with MustMatchers {
  "Result" should {
    "be good" in {
      Result("Hello world", Status.Ok) must have (
        'value (Right("Hello world")),
        'status (Status.Ok)
      )
    }

    "be bad" in {
      Result(Errors("Whoops"), Status.BadRequest) must have (
        'value (Left(Errors("Whoops"))),
        'status (Status.BadRequest)
      )
    }

    "convert good to JSON" in {
      val expectedJson =
        Json.obj(
          "content" -> Json.fromString("Hello world"),
          "status" ->
            Json.obj(
              "code" -> Json.fromInt(Status.Ok.code),
              "reason" -> Json.fromString(Status.Ok.reason)
            )
        )

      Result("Hello world", Status.Ok).asJson mustEqual expectedJson
    }

    "convert bad to JSON" in {
      val expectedJson =
        Json.obj(
          "errors" -> Json.fromValues(Seq(Json.fromString("Whoops"))),
          "status" ->
            Json.obj(
              "code" -> Json.fromInt(Status.BadRequest.code),
              "reason" -> Json.fromString(Status.BadRequest.reason)
            )
        )

      Result(Errors("Whoops"), Status.BadRequest).asJson mustEqual expectedJson
    }

    // TODO - Move into own spec
    /*"" in {
      println(Errors("blah").asJson.as[Errors])
      println(Errors("blah", "blah again").asJson.as[Errors])
    }*/
  }
}