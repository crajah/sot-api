package parallelai.sot.api.entities

import org.scalatest.{MustMatchers, WordSpec}
import parallelai.common.secure.{CryptoMechanic, Encrypted}
import spray.json._
import parallelai.common.secure.diffiehellman.DiffieHellmanClient

class ProductRegisterSpec extends WordSpec with MustMatchers {
  implicit val crypto: CryptoMechanic = new CryptoMechanic(secret = "victorias secret".getBytes)

  "Product register" should {
    "be converted to/from JSON with no client public key" in {
      val productRegister = ProductRegister(Organisation("id", "code", "email"), Encrypted("blah"))
      val productRegisterJson = productRegister.toJson

      println(productRegisterJson)
      println(productRegisterJson.convertTo[ProductRegister])
    }

    "be converted to/from JSON with a client public key" in {
      val productRegister = ProductRegister(Organisation("id", "code", "email"), Encrypted("blah"), Option(DiffieHellmanClient.createClientPublicKey))
      val productRegisterJson = productRegister.toJson

      println(productRegisterJson)
      println(productRegisterJson.convertTo[ProductRegister])
    }
  }
}