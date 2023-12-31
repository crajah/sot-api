package parallelai.sot.api.http.service

import scala.concurrent.Future
import better.files._
import cats.implicits._
import grizzled.slf4j.Logging
import com.softwaremill.sttp._
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.file.GCFileNameConverter._
import parallelai.sot.api.http.ResultOps
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.model.RegisteredVersion

class GetVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends GetVersion[Future] with LicenceEndpointOps with ResultOps with Logging {
  def apply(registeredVersion: RegisteredVersion): Future[String Either File] = {
    val file = (executor.directory / registeredVersion.defineFileName).createIfNotExists(createParents = true)

    val request =
      sttp post licenceUri"/file" body registeredVersion response asFile(file.toJava, overwrite = true)

    info(s"Licence request for version $registeredVersion to get associated file: $file")
    request.send.map(_.body.map(_.toScala))
  }
}