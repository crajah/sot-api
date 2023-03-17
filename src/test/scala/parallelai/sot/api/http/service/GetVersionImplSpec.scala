package parallelai.sot.api.http.service

import java.io.{File, FileInputStream}
import java.nio.file.Files

import com.softwaremill.sttp.testing.SttpBackendStub
import com.twitter.finagle.http.Status
import org.apache.commons.compress.utils.IOUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.sot.api.http.Result
import parallelai.sot.api.model.ZipToken

import scala.concurrent.Future

class GetVersionImplSpec extends WordSpec with MustMatchers with ScalaFutures {

  "make a request for downloading engine source code as Bytes array" in {
    val file = new File("/Users/pasqualegatto/Documents/dev/parallelai/projects/sot-api/README.md.zip")

    implicit val backend: SttpBackendStub[Future, Nothing] = {
      SttpBackendStub.asynchronousFuture
        .whenRequestMatches(_ => true)
        .thenRespond(Result(IOUtils.toByteArray(new FileInputStream(file)), Status.Ok))
    }

    val impl = new GetVersionImpl()

    val future = impl.build(ZipToken("licId", "1.1.5"))

    whenReady(future) { r =>
      new String(Files.readAllBytes(r.right.get.path)) mustEqual ""
    }
  }
}
