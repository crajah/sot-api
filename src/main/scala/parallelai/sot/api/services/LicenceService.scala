package parallelai.sot.api.services

import javax.crypto.SecretKey
import parallelai.common.secure.diffiehellman.ClientSharedSecret

class LicenceService {
  // TODO - Remove this mutable nonsense and use some persistence mechanism
  var licenceId: String = _
  var apiSharedSecret: ClientSharedSecret = _

  var orgCode: String = _
  var orgSharedSecret: SecretKey = _
}

object LicenceService {
  def apply() = new LicenceService
}