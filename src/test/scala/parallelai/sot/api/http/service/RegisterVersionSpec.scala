package parallelai.sot.api.http.service

import java.net.URI
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports._
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{DiffieHellmanClient, DiffieHellmanServer}
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.Result
import parallelai.sot.api.model.{RegisteredVersion, Token, Version}
import parallelai.sot.api.services.{LicenceService, VersionService}

class RegisterVersionSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  "Register version" should {
    "be given a URI to an ecrypted zip of versioned source code" in {
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

      val registerVersion = new RegisterVersionImpl(versionService, licenceService)
      val version = Version(tag, Option(token), Option(DateTime.nextDay))
      val result: Future[Result[Encrypted[RegisteredVersion]]] = registerVersion(Encrypted(version))

      whenReady(result) { r =>
        r.status mustEqual Status.Ok

        versionService.versions mustEqual Map(("organisationCode", tag) -> registeredVersion)
      }
    }
  }
}