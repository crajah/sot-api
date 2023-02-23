package parallelai.sot.api.identity

import org.scalatest.{ MustMatchers, WordSpec }

class IdGeneratorSpec extends WordSpec with MustMatchers {
  "Id Generator" should {
    "generate an Id from nothing" in new IdGenerator {
      uniqueId() must not be empty
    }

    "generate an Id from an original" in new IdGenerator {
      uniqueId("blah") must startWith("blah-")
    }

    "generate an Id from an original with static unique suffix" in new IdGenerator with IdGenerator99UniqueSuffix {
      uniqueId("blah") mustEqual "blah-99"
    }

    "generate an Id from an original with stripped spaced and margin and static unique suffix" in new IdGenerator with IdGenerator99UniqueSuffix {
      uniqueId(" | bla   h  ") mustEqual "blah-99"
    }
  }
}