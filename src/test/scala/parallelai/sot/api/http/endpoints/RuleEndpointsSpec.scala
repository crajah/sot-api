package parallelai.sot.api.http.endpoints

import java.net.URI
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports._
import com.twitter.finagle.http.Status
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.model.{RegisteredVersion, Rule, RuleStatus, Token}
import parallelai.sot.api.services.VersionService


class RuleEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Rule endpoints" should {
    "handle put request with Rule without organisation code" in {
      val versionService = VersionService()
      val endpoint = new RuleEndpoints(versionService) with DatastoreConfig
      import endpoint._
      val rule = Rule("ruleId", version = "ps-to-bq-test_1513181186942", organisation = None)
      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()

      response.status mustEqual Status.Accepted
      response.content.convertTo[RuleStatus] must matchPattern {
        case RuleStatus(_, _, _, _) =>
      }
    }

    "handle put request with Rule with non existing version for a given organisation code" in {
      val organisationCode = "organisationCode"
      val versionService = VersionService()
      val endpoint = new RuleEndpoints(versionService) with DatastoreConfig
      import endpoint._
      val licenceId = "licenceId"
      val tag = "v0.1.12"
      val token = Token(licenceId, organisationCode, "me@gmail.com")
      val registeredVersion = RegisteredVersion(new URI("www.victorias-secret.com"), tag, token, DateTime.now)

      val rule = Rule("ruleId", version = "ps-to-bq-test_1513181186942", organisation = Option(organisationCode))
      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()


      response.status mustEqual Status.BadRequest
      response.content.convertTo[Error] mustEqual Error(s"Non existing version: $organisationCode")

      versionService.versions mustEqual Map()
    }

    "handle put request with Rule with organisation code" in {
      val versionService = VersionService()
      val endpoint = new RuleEndpoints(versionService) with DatastoreConfig
      import endpoint._
      val licenceId = "licenceId"
      val tag = "v0.1.14"
      val organisationCode = "organisationCode"
      val token = Token(licenceId, organisationCode, "me@gmail.com")

      val registeredVersion = RegisteredVersion(new URI("www.victorias-secret.com"), tag, token, DateTime.now)
      val rule = Rule("ruleId", version = tag, organisation = Option(organisationCode))


      versionService.versions += (organisationCode -> tag) -> registeredVersion

      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()

      response.status mustEqual Status.Accepted
      response.content.convertTo[RuleStatus] must matchPattern {
        case RuleStatus(_, _, _, _) =>
      }

      versionService.versions mustEqual Map((organisationCode, tag) -> registeredVersion)
    }
  }
}
