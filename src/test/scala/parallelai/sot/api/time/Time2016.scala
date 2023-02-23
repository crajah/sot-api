package parallelai.sot.api.time

import org.joda.time.Instant
import org.joda.time.Instant._

trait Time2016 extends Time {
  override lazy val now: Instant = parse("2016-06-10T10:11:12Z")
}