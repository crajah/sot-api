package parallelai.sot.api.endpoints

import scala.concurrent.Future
import grizzled.slf4j.Logging
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe._
import io.circe.parser._
import cats.syntax.either._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import monocle.macros.syntax.lens._
import shapeless.HNil
import spray.json._
import spray.json.lenses.JsonLenses._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import com.twitter.finagle.http.Status
import parallelai.common.secure.diffiehellman.{ClientPublicKey, ClientSharedSecret, DiffieHellmanClient, ServerPublicKey}
import parallelai.common.secure.{AES, CryptoMechanic}
import parallelai.sot.api.concurrent.WebServiceExecutionContext
import parallelai.sot.api.config._
import parallelai.sot.api.model.Product

trait ProductEndpoints extends EndpointOps with LicenceEndpointOps with DefaultJsonProtocol with Logging {





  implicit val crypto: CryptoMechanic = new CryptoMechanic(AES, secret = secret.getBytes)

  val productPath: Endpoint[HNil] = api.path :: "product"

  def productEndpoints(implicit sb: SttpBackend[Future, Nothing]) = registerProduct

  protected def registerProduct(implicit sb: SttpBackend[Future, Nothing]): Endpoint[Response] = {
    implicit val ec: WebServiceExecutionContext = WebServiceExecutionContext()

    post(productPath :: "register" :: jsonBody[Product]) { pr: Product =>
      val productRegister = pr.lens(_.clientPublicKey) set Option(createClientPublicKey)

      val request: Request[String, Nothing] =
        sttp post licenceUri"/product/register" body productRegister

      ///////////////////////////////////

      implicit val statusEncoder: Encoder[Status] = (status: Status) =>
        Json.obj(
          ("code", Json.fromInt(status.code)),
          ("reason", Json.fromString(status.reason))
        )

      implicit val statusDecoder: Decoder[Status] = (hcursor: HCursor) => for {
        code <- hcursor.downField("code").as[Int]
      } yield Status(code)

      implicit val responseEncoder: Encoder[Response] = (response: Response) =>
        Json.obj(
          ("content", parse(response.content.compactPrint).right.get),
          ("status", response.status.asJson)
        )

      implicit val responseDecoder: Decoder[Response] = new Decoder[Response] {
        def apply(c: HCursor): Result[Response] = {
          import Response._

          Right(Response(c.downField("content").focus.get.noSpaces.parseJson, c.downField("status").focus.get.noSpaces.parseJson.convertTo[Status]))
        }
      }

      /*(hcursor: HCursor) => for {
        //content <- hcursor.downField("content").focus.get.noSpaces.parseJson
        //status <- hcursor.downField("status").as[Status]
      } yield Response(hcursor.downField("content").focus.get.noSpaces.parseJson, hcursor.downField("status").focus.get.noSpaces.parseJson.convertTo[Status])*/



      ///////////////////////////////////

      /*
      def asJson[B: Decoder]: ResponseAs[Either[io.circe.Error, B], Nothing] =
    asString(Utf8).map(decode[B])
       */

      def asSprayJson[B: JsonReader]: ResponseAs[B, Nothing] = asString("utf-8").map(s => s.parseJson.convertTo[B])

      request.response(asSprayJson[Response]).send.map { response =>
        val rep = response.body
        println(s"===> RRRRR $rep")

        response.body match {
          case Right(r) =>
            println(s"===> THE r IS: $r")
            val serverPublicKey = r.content.convertTo[ServerPublicKey]
            val clientSharedSecret: ClientSharedSecret = DiffieHellmanClient.createClientSharedSecret(serverPublicKey)
            println(s"===> x = $clientSharedSecret")

            Response(serverPublicKey)

          case Left(e) =>
            e.parseJson.convertTo[Response]
        }
      }.toTFuture
    }
  }

  protected def createClientPublicKey: ClientPublicKey = DiffieHellmanClient.createClientPublicKey
}