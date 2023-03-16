package parallelai.sot.api.services

import scala.collection.mutable
import parallelai.sot.api.model.RegisteredVersion

class VersionService {
  type VersionKey = (String, String)

  // TODO - Use persistence instead
  val versions: mutable.Map[VersionKey, RegisteredVersion] =
    mutable.Map[VersionKey, RegisteredVersion]()
}

object VersionService {
  def apply() = new VersionService
}
