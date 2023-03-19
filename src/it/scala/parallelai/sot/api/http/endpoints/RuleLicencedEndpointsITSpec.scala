package parallelai.sot.api.http.endpoints

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files._
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import spray.json._
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports.DateTime
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.ClientSharedSecret
import parallelai.sot.api.config._
import parallelai.sot.api.file.GCFileNameConverter._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.model._
import parallelai.sot.api.services.{LicenceService, VersionService}

class RuleLicencedEndpointsITSpec extends WordSpec with MustMatchers with EndpointOps {
  implicit val licenceService: LicenceService = LicenceService()
  implicit val versionService: VersionService = VersionService()
  implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

  "Licenced rule endpoints" should {
    "fail to build rule because of an invalid rule declaration" in new RuleEndpoints with DatastoreConfig {
      val version = "v0.1.12"
      val organisation = "organisation"
      val token = Token("licenceId3", organisation, "me@gmail.com")
      val uri = new URI("https://www.googleapis.com/uri-not-used-anymore")
      val registeredVersion = RegisteredVersion(uri, version, token, DateTime.nextDay)

      licenceService.apiSharedSecret = ClientSharedSecret(("." / "src" / "it" / "resources" / "secret-test").byteArray)
      versionService.versions += (organisation, version) -> registeredVersion

      val versionToBuild = JsObject(
        "name" -> JsString("my-rule"),
        "version" -> JsString(version),
        "organisation" -> JsString(organisation)
      )

      val Some(response) = buildRule(put(p"/$rulePath/build?registered&wait").withBody[Application.Json](versionToBuild)).awaitValueUnsafe()
      response.status mustEqual Status.UnprocessableEntity
    }

    "build rule" in new RuleEndpoints with DatastoreConfig {
      val version = "v0.1.12"
      val organisation = "organisation"
      val token = Token("licenceId3", organisation, "me@gmail.com")
      val uri = new URI("https://www.googleapis.com/uri-not-used-anymore")
      val registeredVersion = RegisteredVersion(uri, version, token, DateTime.nextDay)

      licenceService.apiSharedSecret = ClientSharedSecret(("." / "src" / "it" / "resources" / "secret-test").byteArray)
      versionService.versions += (organisation, version) -> registeredVersion

      val versionToBuild: JsValue = ("." / "src" / "it" / "resources" / "rule-test.json").contentAsString.parseJson

      val Some(response) = buildRule(put(p"/$rulePath/build?registered&wait").withBody[Application.Json](versionToBuild)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      println(response.content.prettyPrint)

      val file: File = executor.directory / registeredVersion.defineFileName
      file.exists mustBe true

      // TODO - assert the JSON that comes back, of the format:
      /*
      {
        "stage": "BUILD",
        "ident": "RULE: benchmark-1522335510887",
        "id": "1522335685228",
        "msg": "For ruleId benchmark-1522335510887 with version v0.1.12 local source code has been deleted",
        "level": "INFO"
      }
       */
    }
  }
}