package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import io.finch.Application
import io.finch.Input._
import io.finch.circe._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import com.google.cloud.datastore.DatastoreOptions
import com.softwaremill.sttp.Request
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.gcp.datastore.{DatastoreConfig, DatastoreConfigMock}
import parallelai.sot.api.model.{Token, Version}
import parallelai.sot.api.config.{licence, secret}
import parallelai.sot.api.http.{Errors, Result}
import com.github.nscala_time.time.Imports._

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

      new VersionEndpoints with DatastoreConfigMock {
        val expiry = DateTime.nextDay
        val token = Token("licenceId", "organisationCode", "me@gmail.com")
        val version = Version("1.1.4", Option(token), Option(expiry))
        val Some(Result(Right(registeredVersion), Status.Ok)) = register(post(p"/$versionPath/register").withBody(Encrypted(version))).awaitValueUnsafe()

        println("RESULT: " + registeredVersion.decrypt)
      }
    }
  }
}

// case class Version(licenseId: String, orgCode: String, value: String, expiriy: String)