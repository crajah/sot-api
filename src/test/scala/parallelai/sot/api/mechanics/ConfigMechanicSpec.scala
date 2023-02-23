package parallelai.sot.api.mechanics

import spray.json._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.sot.api.config._

class ConfigMechanicSpec extends WordSpec with MustMatchers with ScalaFutures with ConfigMechanic {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  "Configuration" should {
    "be created" in {
      val json = JsObject("name" -> JsString(""), "version" -> JsString(""))
      val rulePath = baseDirectory / "rule-path"
      val ruleName = "rule-name"

      whenReady(createConfiguration(json, rulePath, ruleName)) {
        case (applicationConfig, rule) =>
          applicationConfig.file mustEqual baseDirectory / "rule-path" / executor.configuration.resourcePath.toString / "application.conf"
          applicationConfig.file.contentAsString.parseJson mustEqual JsObject("json" -> JsObject("file" -> JsObject("name" -> JsString(s"$ruleName.json"))))

          rule.file.contentAsString.parseJson mustEqual json
      }
    }
  }
}