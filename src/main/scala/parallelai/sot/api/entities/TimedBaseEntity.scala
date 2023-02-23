package parallelai.sot.api.entities

import org.joda.time.Instant

trait TimedBaseEntity extends IdentityEntity {
  def timestamp: Option[Instant]
}