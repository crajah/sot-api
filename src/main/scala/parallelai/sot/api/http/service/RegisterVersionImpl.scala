package parallelai.sot.api.http.service

import java.net.URI
import scala.concurrent.Future
import cats.implicits._
import org.joda.time.DateTime
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.http.{Result, ResultOps}
import parallelai.sot.api.model.{RegisteredVersion, Token, Version}

class RegisterVersionImpl(implicit sb: SttpBackend[Future, Nothing]) extends RegisterVersion[Future] with LicenceEndpointOps with ResultOps {
  def apply(versionToken: Encrypted[Version]): Future[Result[Encrypted[RegisteredVersion]]] =
    Future successful Result(Encrypted(RegisteredVersion(new URI(""), Token("", "", ""), new DateTime()), Crypto(AES, secret.getBytes)), Status.Ok)
}