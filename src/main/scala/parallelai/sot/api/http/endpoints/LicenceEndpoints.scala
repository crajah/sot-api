package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.circe._
import io.finch.syntax._
import monocle.macros.syntax.lens._
import shapeless.HNil
import com.softwaremill.sttp._
import parallelai.common.secure.diffiehellman.{ClientPublicKey, DiffieHellmanClient}
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.config._
import parallelai.sot.api.http.Result
import parallelai.sot.api.http.service.{RegisterOrganisationImpl, RegisterProductImpl}
import parallelai.sot.api.model.{Organisation, Product, RegisteredOrganisation, RegisteredProduct, Token}
import parallelai.sot.api.services.LicenceService

class LicenceEndpoints(licenceService: LicenceService)(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with Logging {
  lazy val registerProduct = new RegisterProductImpl(licenceService)
  lazy val registerOrganisation = new RegisterOrganisationImpl

  val productPath: Endpoint[HNil] = api.path :: "product"

  val organisationPath: Endpoint[HNil] = api.path :: "org"

  lazy val licenceEndpoints = productRegistation :+: organisationRegistation

  lazy val productRegistation: Endpoint[Result[RegisteredProduct]] =
    post(productPath :: "register" :: jsonBody[Product]) { product: Product =>
      registerProduct(product.lens(_.clientPublicKey) set Option(createClientPublicKey)).toTFuture
    }

  lazy val organisationRegistation: Endpoint[Result[RegisteredOrganisation]] =
    post(organisationPath :: "register" :: jsonBody[Organisation]) { organisation: Organisation =>
      // TODO - License ID should not be encrypted
      val token = Token(licenceService.licenceId, organisation.code, organisation.email)

      registerOrganisation(organisation.lens(_.token) set Some(Encrypted(token, Crypto(AES, licenceService.apiSharedSecret.value)))).toTFuture
    }

  protected def createClientPublicKey: ClientPublicKey =
    DiffieHellmanClient.createClientPublicKey
}

object LicenceEndpoints {
  def apply(licenceService: LicenceService)(implicit sb: SttpBackend[Future, Nothing]) =
    new LicenceEndpoints(licenceService).licenceEndpoints
}