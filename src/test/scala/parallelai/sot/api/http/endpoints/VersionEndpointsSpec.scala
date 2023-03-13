package parallelai.sot.api.http.endpoints

import com.github.nscala_time.time.Imports._
import com.softwaremill.sttp.Request
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import io.finch.Input._
import io.finch.circe._
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.config.{licence, secret}
import parallelai.sot.api.gcp.datastore.DatastoreConfigMock
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.model.{RegisteredVersion, Token, Version}
import parallelai.sot.api.services.VersionService

import scala.concurrent.Future

class VersionEndpointsSpec extends WordSpec with MustMatchers {
  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  val licenceHostExpectation: Request[_, _] => Boolean =
    _.uri.host.contains(licence.name)

  val registerVersionPathExpectation: Request[_, _] => Boolean =
    _.uri.path.startsWith(Seq(licence.context, licence.version, "version", "register"))

  "Version endpoints" should {
    "register a version" in {
      implicit val backend: SttpBackendStub[Future, Nothing] =
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(req => licenceHostExpectation(req) && registerVersionPathExpectation(req))
          .thenRespond(Result(Errors(""), Status.Unauthorized))

      val versionService = VersionService()

      new VersionEndpoints(versionService) with DatastoreConfigMock {
        val expiry = DateTime.nextDay
        val token = Token("licenceId", "organisationCode", "me@gmail.com")
        val version = Version("1.1.4", Option(token), Option(expiry))
        val Some(Result(Right(registeredVersion), Status.Ok)) = register(post(p"/$versionPath/register").withBody(Encrypted(version))).awaitValueUnsafe()

        registeredVersion.decrypt must matchPattern {
          case RegisteredVersion(_, _, _) =>
        }

        versionService.versions mustEqual Map(("organisationCode", "1.1.4") -> version)
      }
    }
  }
}