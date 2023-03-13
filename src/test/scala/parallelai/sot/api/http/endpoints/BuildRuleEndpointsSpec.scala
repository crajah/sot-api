package parallelai.sot.api.http.endpoints

import com.twitter.finagle.http.Status
import io.finch.Application
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import io.finch.Input._
import io.finch.sprayjson._
import parallelai.sot.api.model.Rule

class BuildRuleEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Rule endpoints" should {
    "handle put request" in new RuleEndpoints with DatastoreConfig {

      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](Rule("ruleId", version = "ps-to-bq-test_1513181186942", organization = "code"))).awaitValueUnsafe()

      println(response.content)
      response.status mustEqual Status.Accepted
    }
  }
}
