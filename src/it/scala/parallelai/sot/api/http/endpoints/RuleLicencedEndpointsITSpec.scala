package parallelai.sot.api.http.endpoints

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files._
import cats.implicits._
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore._
import spray.json.{JsObject, JsString, JsValue}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.github.nscala_time.time.Imports.DateTime
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.twitter.finagle.http.Status
import parallelai.sot.api.config.baseDirectory
import parallelai.sot.api.gcp.datastore.{DatastoreConfig, DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.mechanics._
import parallelai.sot.api.model.Files._
import parallelai.sot.api.model.{IdGenerator99UniqueSuffix, _}
import parallelai.sot.api.services.{LicenceService, VersionService}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class RuleLicencedEndpointsITSpec extends WordSpec with MustMatchers with EndpointOps {
  implicit val licenceService: LicenceService = LicenceService()
  implicit val versionService: VersionService = VersionService()
  implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

  "Licenced rule endpoints" should {
    "build rule" in new RuleEndpoints with DatastoreConfig {
      val version = "v0.1.12"
      val organisation = "organisation"
      val token = Token("licenceId3", organisation, "me@gmail.com")
      val uri = new URI("https://www.googleapis.com/uri-not-used-anymore")
      val registeredVersion = RegisteredVersion(uri, version, token, DateTime.nextDay)

      versionService.versions += (organisation, version) -> registeredVersion

      // TODO - WIP
      //buildRule(registeredVersion)

      val versionToBuild = JsObject(
        "name" -> JsString("my-rule"),
        "version" -> JsString(version),
        "organisation" -> JsString(organisation)
      )

      val Some(response) = buildRule(put(p"/$rulePath/build?registered").withBody[Application.Json](versionToBuild)).awaitValueUnsafe()
      response.status mustEqual Status.Accepted
    }
  }
}