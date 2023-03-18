package parallelai.sot.api.http.service

import java.util.UUID
import scala.concurrent.Future
import better.files._
import cats.implicits._
import com.softwaremill.sttp._
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.baseDirectory
import parallelai.sot.api.http.ResultOps
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.model.RegisteredVersion

class GetVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends GetVersion[Future] with LicenceEndpointOps with ResultOps {
  def apply(registeredVersion: RegisteredVersion): Future[String Either File] = {
    val request =
      sttp post licenceUri"/file" body registeredVersion response asFile((baseDirectory / s"${UUID.randomUUID().toString}.zip").createIfNotExists(createParents = true).toJava, overwrite = true)
      // TODO - Sort out "random ID" !!! Unable to write to...

    println(s"===> API: sending request to licence for $registeredVersion")
    request.send.map(_.body.map(_.toScala))
  }
}