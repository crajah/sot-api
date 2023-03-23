package parallelai.sot.api.services

import javax.crypto.SecretKey

class OrganisationService {
  // TODO - Remove this mutable nonsense and use some persistence mechanism
  var orgCode: String = _
  var orgSharedSecret: SecretKey = _
}

object OrganisationService {
  def apply() = new OrganisationService
}