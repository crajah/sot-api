package parallelai.sot.api.http.endpoints

import java.net.URI
import scala.concurrent.Future
import io.finch.Input._
import io.finch.circe._
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports._
import com.softwaremill.sttp.Request
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{DiffieHellmanClient, DiffieHellmanServer}
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.config.{licence, secret}
import parallelai.sot.api.gcp.datastore.DatastoreConfigMock
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.model.{RegisteredVersion, Token, Version}
import parallelai.sot.api.services.{LicenceService, VersionService}

class VersionEndpointsSpec extends WordSpec with MustMatchers {
  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  implicit val backend: SttpBackendStub[Future, Nothing] = SttpBackendStub.asynchronousFuture

  val licenceHostExpectation: Request[_, _] => Boolean =
    _.uri.host.contains(licence.name)

  val registerVersionPathExpectation: Request[_, _] => Boolean =
    _.uri.path.startsWith(Seq(licence.context, licence.version, "version", "register"))

  "Version endpoints" should {
    "does not register a version if expired" in {
      val licenceId = "licenceId"
      val versionService = VersionService()
      val licenceService = LicenceService()
      val (serverPublicKey, _) = DiffieHellmanServer create DiffieHellmanClient.createClientPublicKey

      licenceService.licenceId = licenceId
      licenceService.apiSharedSecret = DiffieHellmanClient createClientSharedSecret serverPublicKey

      val tag = "v0.1.12"
      val token = Token(licenceId, "organisationCode", "me@gmail.com")

      val apiSharedCrypto = Crypto(AES, licenceService.apiSharedSecret.value)

      val registeredVersion = RegisteredVersion(new URI("www.victorias-secret.com"), tag, token, DateTime.now)

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(_ => true)
          .thenRespond(Result(Encrypted(registeredVersion, apiSharedCrypto), Status.Ok))
      }

      new VersionEndpoints(licenceService, versionService) with DatastoreConfigMock {
        val expiry: DateTime = DateTime.yesterday()
        val token = Token("licenceId", "organisationCode", "me@gmail.com")
        val version = Version("1.1.4", Option(token), Option(expiry))

        val Some(response) = register(post(p"/$versionPath/register").withBody(Encrypted(version))).awaitValueUnsafe()

        response must matchPattern { case Result(Left(Errors("Version expired!")), Status(422)) => }
      }
    }

    "register a version" in {
      val licenceId = "licenceId"
      val versionService = VersionService()
      val licenceService = LicenceService()
      val (serverPublicKey, _) = DiffieHellmanServer create DiffieHellmanClient.createClientPublicKey

      licenceService.licenceId = licenceId
      licenceService.apiSharedSecret = DiffieHellmanClient createClientSharedSecret serverPublicKey

      val tag = "v0.1.12"
      val token = Token(licenceId, "organisationCode", "me@gmail.com")

      val apiSharedCrypto = Crypto(AES, licenceService.apiSharedSecret.value)

      val registeredVersion = RegisteredVersion(new URI("www.victorias-secret.com"), tag, token, DateTime.nextDay())

      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(_ => true)
          .thenRespond(Result(Encrypted(registeredVersion, apiSharedCrypto), Status.Ok))
      }

      new VersionEndpoints(licenceService, versionService) with DatastoreConfigMock {
        val expiry: DateTime = DateTime.nextDay
        val token = Token("licenceId", "organisationCode", "me@gmail.com")
        val version = Version("1.1.4", Option(token), Option(expiry))

        val Some(Result(Right(registeredVersion), Status.Ok)) = register(post(p"/$versionPath/register").withBody(Encrypted(version))).awaitValueUnsafe()

        registeredVersion.decrypt(apiSharedCrypto) must matchPattern { case RegisteredVersion(_, _, _, _) => }
      }
    }
  }
}