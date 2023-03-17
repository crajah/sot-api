package parallelai.sot.api.http.service

import java.net.URI
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports.DateTime
import com.softwaremill.sttp.testing.SttpBackendStub
import parallelai.sot.api.config._
import parallelai.sot.api.model.{RegisteredVersion, Token}

class GetVersionSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Get version" should {
    "make a request for downloading engine source code" in {
      implicit val backend: SttpBackendStub[Future, Nothing] = {
        SttpBackendStub.asynchronousFuture
          .whenRequestMatches(_ => true)
          .thenRespond((baseDirectory / "test.txt").createIfNotExists(createParents = true).writeText("Testing 1, 2, 3").toJava)
      }

      val getVersion = new GetVersionImpl

      val token = Token("licenceId", "organisationCode", "me@gmail.com")
      val uri = new URI("https://www.googleapis.com/download/storage/v1/b/sot-rules/o/licenceId-parallelai-sot-v0-encrypted.zip?generation=1522091908107420&alt=media") // TODO - Pointless
      val registeredVersion = RegisteredVersion(uri, "v0.1.14", token, DateTime.nextDay)

      whenReady(getVersion(registeredVersion)) {
        case Right(file) => file.contentAsString mustEqual "Testing 1, 2, 3"
      }
    }
  }
}
