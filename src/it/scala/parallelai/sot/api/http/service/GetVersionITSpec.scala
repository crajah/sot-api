package parallelai.sot.api.http.service

import java.net.URI
import scala.concurrent.Future
import better.files.File
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports.DateTime
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import parallelai.sot.api.model.{RegisteredVersion, Token}

class GetVersionITSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

  val getVersion = new GetVersionImpl

  "Get version" should {
    "get encrypted zip file" in {
      val token = Token("licenceId", "organisationCode", "me@gmail.com")
      val uri = new URI("https://www.googleapis.com/download/storage/v1/b/sot-rules/o/licenceId-parallelai-sot-v0-encrypted.zip?generation=1522091908107420&alt=media")
      val registeredVersion = RegisteredVersion(uri, "v0.1.14", token, DateTime.nextDay)

      implicit val okSttpFutureBackend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

      whenReady(getVersion(registeredVersion)) { eitherStringOrFile =>
        println(eitherStringOrFile) // TODO - We get Left(Invalid Credentials)
      }
    }
  }
}