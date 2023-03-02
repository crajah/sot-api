package parallelai.sot.api.entities

import spray.json._
import org.scalatest.{MustMatchers, WordSpec}
import parallelai.common.secure.diffiehellman.DiffieHellmanClient
import parallelai.common.secure.{CryptoMechanic, Encrypted}

class ProductRegisterSpec extends WordSpec with MustMatchers {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = "victorias secret".getBytes)

  "Product register" should {
    "be converted to/from JSON with no client public key" in {
      val organisation = Organisation("id", "code", "email")
      val productToken = Encrypted("blah")
      val productRegister = ProductRegister(organisation, productToken)
      val productRegisterJson = productRegister.toJson

      val ProductRegister(o, p, c) = productRegisterJson.convertTo[ProductRegister]

      o mustEqual organisation
      new String(p.decrypt) mustEqual "blah"
      c mustBe None
    }

    "be converted to/from JSON with a client public key" in {
      val organisation = Organisation("id", "code", "email")
      val productToken = Encrypted("blah")
      val clientPublicKey = DiffieHellmanClient.createClientPublicKey
      val productRegister = ProductRegister(organisation, productToken, Option(clientPublicKey))
      val productRegisterJson = productRegister.toJson

      val ProductRegister(o, p, c) = productRegisterJson.convertTo[ProductRegister]

      o mustEqual organisation
      new String(p.decrypt) mustEqual "blah"

      c.get.value mustEqual clientPublicKey.value
      c.get.keyPair.getPublic mustEqual clientPublicKey.keyPair.getPublic
      c.get.keyPair.getPrivate mustEqual clientPublicKey.keyPair.getPrivate
    }
  }
}