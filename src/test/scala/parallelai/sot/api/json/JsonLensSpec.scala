package parallelai.sot.api.json

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.lenses.JsonLenses._
import org.scalatest.{ MustMatchers, WordSpec }

class JsonLensSpec extends WordSpec with MustMatchers {
  "Json lenses" should {
    "find an optional field" in {
      val json = JsObject(
        "id" -> JsString("my-id"),
        "name" -> JsString("my-name"))

      json.extract[String]('id.?) mustBe Some("my-id")
    }

    "not find an optional field" in {
      val json = JsObject(
        "name" -> JsString("my-name"))

      json.extract[String]('id.?) mustBe None
    }
  }
}