package parallelai.sot.api.http.service

import scala.concurrent.Future
import cats.implicits._
import com.softwaremill.sttp.{Request, SttpBackend, sttp}
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.http.{Errors, Result, ResultOps}
import parallelai.sot.api.model.{RegisteredVersion, Version}
import parallelai.sot.api.services.{LicenceService, VersionService}

class RegisterVersionImpl(versionService: VersionService, licenceService: LicenceService)(implicit sb: SttpBackend[Future, Nothing]) extends RegisterVersion[Future] with LicenceEndpointOps with ResultOps {
  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  def apply(version: Encrypted[Version]): Future[Result[Encrypted[RegisteredVersion]]] = {
    val request: Request[Result[Encrypted[RegisteredVersion]], Nothing] =
      sttp post licenceUri"/version/register" body version response asJson[Result[Encrypted[RegisteredVersion]]]

    request.send.map { response =>
      response.body match {
        case Right(result @ Result(Right(encryptedRegisteredVersion), _)) =>
          validateExpiry(result, encryptedRegisteredVersion.decrypt(Crypto(AES, licenceService.apiSharedSecret.value)))

        case Right(result @ Result(Left(_), _)) => result

        case Left(error) => Result(Left(Errors(error)), Status.UnprocessableEntity)
      }
    }
  }

  private def validateExpiry(result: Result[Encrypted[RegisteredVersion]], registeredVersion: RegisteredVersion) = {
    if (registeredVersion.expiry.isBeforeNow) Result[Encrypted[RegisteredVersion]](Left(Errors("Version expired!")), Status.UnprocessableEntity) else {
      versionService.versions += ((registeredVersion.token.code, registeredVersion.version) -> registeredVersion)
      result
    }
  }
}