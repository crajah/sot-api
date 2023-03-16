package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files.File
import cats.implicits._
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.sot.api.config.executor
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.model.{GitVersion, Version}
import parallelai.sot.api.services.{LicenceService, VersionService}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class VersionEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures
                             with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                             with EndpointOps with DatastoreMappableType {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture

  val container: Container = datastoreContainer

  val licenceService = LicenceService()
  val versionService = VersionService()

  "Version endpoints" should {
    "have no available versions and so no links to other version APIs" in new VersionEndpoints(licenceService, versionService) with DatastoreITConfig {
      val Some(response) = versions(get(p"/$versionPath")).awaitValueUnsafe()

      // TODO
      println(response)
      // Response({"versions":[]},Status(200))
    }

    /*"get all available versions" in new VersionEndpoints with DatastoreITConfig {
      val Some(Inr(Inl(response))) = versionEndpoints(get(p"/$versionPath")).awaitValueUnsafe()

      println(response)
      //response mustEqual Response(JsObject("error-message" -> JsString(s"""Invalid JSON, Object expected in field 'id', for provided rule: "$jsonString"""")), Status.BadRequest)
    }*/

    "fail to refresh" in new VersionEndpoints(licenceService, versionService) with DatastoreITConfig {
      val version = Version("v0.1.4")
      val Some(response) = refreshVersion(post(p"/$versionPath/refresh?wait=true").withBody[Application.Json](version)).awaitValueUnsafe()

      response.status mustEqual Status.NotFound
      response.content.convertTo[Error] mustEqual Error(s"No git repository to reload at ${executor.directory / "git" / version.value}")
    }

    "refresh" in new VersionEndpoints(licenceService, versionService) with DatastoreITConfig {
      val version = Version("v0.1.4")
      val `v0.1.4 directory`: File = executor.directory / "git" / version.value
      val `v0.1.4 directory of executor`: File = `v0.1.4 directory` / "sot-executor"
      val `v0.1.4 directory of executor name`: String = `v0.1.4 directory of executor`.pathAsString

      override protected def codeFromEngineRepo(version: String): Future[File] =
        `v0.1.4 directory of executor`.createDirectories.pure[Future]

      `v0.1.4 directory`.createDirectories

      val Some(response) = refreshVersion(post(p"/$versionPath/refresh?wait=true").withBody[Application.Json](version)).awaitValueUnsafe()

      response.status mustEqual Status.Ok
      response.content.convertTo[GitVersion] must matchPattern { case GitVersion("v0.1.4", `v0.1.4 directory of executor name`, true) => }
    }
  }
}