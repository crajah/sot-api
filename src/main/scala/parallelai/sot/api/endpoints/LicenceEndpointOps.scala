package parallelai.sot.api.endpoints

import com.softwaremill.sttp.{Uri, _}
import parallelai.sot.api.config.licence

trait LicenceEndpointOps {
  implicit class LicencePathContext(val sc: StringContext) {
    def licenceUri(args: Any*): Uri = {
      val path = s"${licence.uri}${sc.s(args: _*)}?key=${licence.apiKey}"
      uri"$path"
    }
  }
}