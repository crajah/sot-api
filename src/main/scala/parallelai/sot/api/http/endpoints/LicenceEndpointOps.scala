package parallelai.sot.api.http.endpoints

import com.softwaremill.sttp.{Uri, _}
import parallelai.sot.api.config.licence

trait LicenceEndpointOps {
  implicit class LicencePathContext(val sc: StringContext) {
    def licenceUri(args: Any*): Uri = {
      val path = s"http://localhost:8081/licence/2${sc.s(args: _*)}"
      uri"$path?key=${licence.apiKey}"
    }
  }
}