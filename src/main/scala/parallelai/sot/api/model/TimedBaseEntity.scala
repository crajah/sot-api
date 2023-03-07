package parallelai.sot.api.model

import org.joda.time.Instant

trait TimedBaseEntity extends IdentityEntity {
  def timestamp: Option[Instant]
}