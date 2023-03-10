package parallelai.sot.api.http.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.finch._
import io.finch.circe._
import io.finch.syntax._
import shapeless.HNil
import com.softwaremill.sttp._
import parallelai.sot.api.config._
import parallelai.sot.api.http.Result
import parallelai.sot.api.http.service.{RegisterOrganisationImpl, RegisterProductImpl}
import parallelai.sot.api.model.{Organisation, Product, RegisteredOrganisation, RegisteredProduct, Token}
import monocle.macros.syntax.lens._
import parallelai.common.secure.{AES, CryptoMechanic, Encrypted}
import parallelai.common.secure.diffiehellman.{ClientPublicKey, DiffieHellmanClient}

class LicenceEndpoints(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with Logging {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  lazy val registerProduct = new RegisterProductImpl
  lazy val registerOrganisation = new RegisterOrganisationImpl

  val productPath: Endpoint[HNil] = api.path :: "product"

  val organisationPath: Endpoint[HNil] = api.path :: "org"

  lazy val licenceEndpoints = productRegistation :+: organisationRegistation

  lazy val productRegistation: Endpoint[Result[RegisteredProduct]] =
    post(productPath :: "register" :: jsonBody[Product]) { product: Product =>
      val result: Future[Result[RegisteredProduct]] = registerProduct(product.lens(_.clientPublicKey) set Option(createClientPublicKey))

      result.toTFuture
    }

  lazy val organisationRegistation: Endpoint[Result[RegisteredOrganisation]] =
    post(organisationPath :: "register" :: jsonBody[Organisation]) { organisation: Organisation =>
      val token = Token(registerProduct.licenceId, organisation.code, organisation.email)
      // TODO: SHould be encrypted using the API_SHARED_SECRET from the previous step. NOT THE API_SRV_SECRET!
      // TODO: License ID should not be encrypted
      val result: Future[Result[RegisteredOrganisation]] = registerOrganisation(organisation.lens(_.token) set Some(Encrypted(token)))

      result.toTFuture
    }

  protected def createClientPublicKey: ClientPublicKey =
    DiffieHellmanClient.createClientPublicKey
}

object LicenceEndpoints {
  def apply()(implicit sb: SttpBackend[Future, Nothing]) =
    (new LicenceEndpoints).licenceEndpoints
}