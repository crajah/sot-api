package parallelai.sot.api.http.service

import scala.concurrent.Future
import cats.implicits._
import javax.crypto.SecretKey
import com.softwaremill.sttp.{Request, SttpBackend, sttp}
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto}
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.sot.api.concurrent.ExecutionContexts.webServiceExecutionContext
import parallelai.sot.api.config.secret
import parallelai.sot.api.http.endpoints.LicenceEndpointOps
import parallelai.sot.api.http.{Errors, Result, ResultOps}
import parallelai.sot.api.model.{Organisation, RegisteredOrganisation}
import parallelai.sot.api.services.LicenceService

class RegisterOrganisationImpl(implicit licenceService: LicenceService, sb: SttpBackend[Future, Nothing]) extends RegisterOrganisation[Future] with LicenceEndpointOps with ResultOps {
  implicit val crypto: Crypto = Crypto(AES, secret.getBytes)

  def apply(organisation: Organisation): Future[Result[RegisteredOrganisation]] = {
    val request: Request[Result[RegisteredOrganisation], Nothing] =
      sttp post licenceUri"/org/register" body organisation response asJson[Result[RegisteredOrganisation]]

    request.send.map { response =>
      response.body match {
        case Right(result @ Result(Right(registeredOrganisation), status)) =>
          val sharedSecret = registeredOrganisation.orgSharedSecret.decrypt(Crypto(AES, licenceService.apiSharedSecret.value))
          licenceService.orgCode = sharedSecret.id
          licenceService.orgSharedSecret = sharedSecret.secret

          result

        case Right(result @ Result(Left(errors), status)) =>
          result

        case Left(error) =>
          Result(Left(Errors(error)), Status.UnprocessableEntity)
      }
    }
  }

  protected def createClientPublicKey: ClientPublicKey =
    DiffieHellmanClient.createClientPublicKey

  protected def createClientSharedSecret(serverPublicKey: ServerPublicKey): ClientSharedSecret =
    DiffieHellmanClient createClientSharedSecret serverPublicKey
}