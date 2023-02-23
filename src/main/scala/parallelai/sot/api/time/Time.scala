package parallelai.sot.api.time

import org.joda.time.Instant

trait Time {
  def now: Instant = org.joda.time.Instant.now
}