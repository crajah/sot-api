package parallelai.sot.api.http.endpoints

import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.api.config.licence

class LicenceEndpointOpsSpec extends WordSpec with MustMatchers with LicenceEndpointOps {
  "Licence endpoints" should {
    "construct URI" in {
      licenceUri"/health/ye".toString mustEqual s"${licence.uri}/health/ye?key=${licence.apiKey.get}"
    }
  }
}