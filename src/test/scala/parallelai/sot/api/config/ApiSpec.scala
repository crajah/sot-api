package parallelai.sot.api.config

import java.net.URI
import org.scalatest.{ MustMatchers, WordSpec }

class ApiSpec extends WordSpec with MustMatchers {
  "Configuration of API" should {
    "be configured" in {
      api.uri mustEqual new URI("http://0.0.0.0:8880")
    }
  }
}
