package parallelai.sot.api.http.endpoints

import java.net.URI
import scala.concurrent.Future
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.twitter.finagle.http.Status
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.model.{RegisteredVersion, Rule, RuleStatus, Token}
import parallelai.sot.api.services.{LicenceService, OrganisationService, VersionService}

class RuleEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit val licenceService: LicenceService = LicenceService()
  implicit val organisationService: OrganisationService = OrganisationService()
  implicit val versionService: VersionService = VersionService()
  implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

  "Rule endpoints" should {
    "handle request with Rule without organisation code" in {
      val rule = Rule("ruleId", version = "ps-to-bq-test_1513181186942", organisation = None)

      new RuleEndpoints with DatastoreConfig {
        val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()

        response.status mustEqual Status.Accepted
        response.content.convertTo[RuleStatus] must matchPattern { case RuleStatus(_, _, _, _) => }
      }
    }

    "handle request with Rule with non existing version for a given organisation code" in {
      val organisationCode = "organisationCode"
      val licenceId = "licenceId"
      val tag = "v0.1.12"
      val token = Token(licenceId, organisationCode, "me@gmail.com")
      val registeredVersion = RegisteredVersion(new URI("www.victorias-secret.com"), tag, token, DateTime.now)

      new RuleEndpoints with DatastoreConfig {
        val rule = Rule("ruleId", version = tag, organisation = Option(organisationCode))
        val response = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()

        println(s"===> $response")

        // TODO Broken because haven't yet stubbed web service call
        /*result.status mustEqual Status.BadRequest
        result.value.left.get mustEqual Errors("xxxx")
        versionService.versions mustEqual Map()*/
      }
    }

    // TODO - WIP
    /*"handle request with organisation code" in {
      val versionService = VersionService()
      val licenceId = "licenceId"
      val tag = "v0.1.14"
      val organisationCode = "organisationCode"
      val token = Token(licenceId, organisationCode, "me@gmail.com")

      val registeredVersion = RegisteredVersion(new URI("www.victorias-secret.com"), tag, token, DateTime.now)
      val rule = Rule("ruleId", version = tag, organisation = Option(organisationCode))

      versionService.versions += (organisationCode -> tag) -> registeredVersion

      new RuleEndpoints with DatastoreConfig {
        val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](rule)).awaitValueUnsafe()

        response.status mustEqual Status.Accepted
        response.content.convertTo[RuleStatus] must matchPattern { case RuleStatus(_, _, _, _) => }

        versionService.versions mustEqual Map((organisationCode, tag) -> registeredVersion)
      }
    }*/
  }
}
