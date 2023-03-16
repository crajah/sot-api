package parallelai.sot.api.services

import parallelai.common.secure.diffiehellman.ClientSharedSecret

class LicenceService {
  // TODO - Remove this mutable nonsense and use some persistence mechanism
  var licenceId: String = _
  var apiSharedSecret: ClientSharedSecret = _
}

object LicenceService {
  def apply() = new LicenceService
}