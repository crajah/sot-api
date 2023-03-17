package parallelai.sot.api.http.service

import scala.concurrent.Future
import better.files._
import cats.implicits._
import com.softwaremill.sttp.{SttpBackend, Uri, asFile, sttp}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.baseDirectory
import parallelai.sot.api.model.RegisteredVersion

class GetVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends GetVersion[Future] {
  def apply(registeredVersion: RegisteredVersion): Future[String Either File] = {
    val request =
      sttp header("Authorization", "Bearer: AIzaSyD5P5f62toM_xzfMgO6K2kEEiomQM_FD4o") get Uri(registeredVersion.uri) response asFile((baseDirectory / "temp").createIfNotExists(createParents = true).toJava)

    request.send.map(_.body.map(_.toScala))
  }
}