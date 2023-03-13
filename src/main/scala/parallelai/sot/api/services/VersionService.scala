package parallelai.sot.api.services

import parallelai.sot.api.model.Version

import scala.collection.mutable

class VersionService {
  type VersionKey = (String, String)

  // TODO - Use persistence instead
  val versions: mutable.Map[VersionKey, Version] =
    mutable.Map[VersionKey, Version]()
}

object VersionService {
  def apply() = new VersionService
}
