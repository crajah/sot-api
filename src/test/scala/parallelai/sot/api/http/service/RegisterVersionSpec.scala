package parallelai.sot.api.http.service

import java.net.URI
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports._
import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.Result
import parallelai.sot.api.model.{RegisteredVersion, Token, Version}

class RegisterVersionSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  "Register version" should {
    "be given a URI to an ecrypted zip of versioned source code" in {
      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(_ => true)
          .thenRespond(Result(Encrypted(RegisteredVersion(new URI(""), Token("", "", ""), new DateTime()), Crypto(AES, secret.getBytes)), Status.Ok))
      }

      val registerVersion = new RegisterVersionImpl

      val version = Version("v0.1.12", Option(Token("licenceId", "organisationCode", "me@gmail.com")), Option(DateTime.nextDay))

      val result: Future[Result[Encrypted[RegisteredVersion]]] = registerVersion(Encrypted(version))

      whenReady(result) { r =>
        r.status mustEqual Status.Ok


        println(r.value)
        // TODO - Copied the following from RegisterProductSpec
        /*r.value.right.get mustEqual registeredProduct

        registerProduct.licenceId mustEqual "licenceId"*/
      }
    }
  }
}