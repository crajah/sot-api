package parallelai.sot.api.http.endpoints

import com.twitter.finagle.http.Status
import io.finch.Application
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import io.finch.Input._
import io.finch.sprayjson._
import parallelai.sot.api.model.{Rule, RuleStatus}

class BuildRuleEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Rule endpoints" should {
    "handle put request with Rule" in new RuleEndpoints with DatastoreConfig {
      val rule = Rule("ruleId", version = "ps-to-bq-test_1513181186942", organization = "code")
      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()

      response.status mustEqual Status.Accepted
      response.content.convertTo[RuleStatus] must matchPattern {
        case RuleStatus(_, _, _, _) =>
      }
    }
  }
}
