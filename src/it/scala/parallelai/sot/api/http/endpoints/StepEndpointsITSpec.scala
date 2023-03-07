package parallelai.sot.api.http.endpoints

import java.util.Base64
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.twitter.finagle.http.Status
import parallelai.sot.api.model.JsonFormats
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture
import parallelai.sot.executor.model.SOTMacroConfig.TransformationOp

class StepEndpointsITSpec extends WordSpec with MustMatchers with Inside with ScalaFutures
                          with EndpointOps with DatastoreMappableType with JsonFormats with CollectionFormats with DefaultJsonProtocol
                          with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val transformation = """m => m.append('count, datastore1.get[Message]("blah").map(_.score).getOrElse(1))"""

  val encoded: Array[Byte] = Base64.getEncoder.encode(transformation.getBytes)

  val transformationStep = JsObject(
    "type" -> JsString("transformation"),
    "id" -> JsString("id"),
    "name" -> JsString("mapper1"),
    "op" -> JsString("map"),
    "params" -> Seq(Seq(new String(encoded))).toJson,
    "paramsEncoded" -> JsBoolean(false)
  )

  "Step endpoints" should {
    "create a new TransformationOp" in new StepEndpoints with DatastoreITConfig {
      val Some(response) = registerStep(post(p"/$stepPath").withBody[Application.Json](transformationStep)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      val actual: TransformationOp = response.content.convertTo[TransformationOp]

      val expectedParams = new String(encoded)

      actual must matchPattern {
        case TransformationOp("transformation", _, "mapper1", "map", Seq(Seq(`expectedParams`)), false) =>
      }

      whenReady(stepDAO findAll) { registeredOpTypeWrapper =>
        registeredOpTypeWrapper.head.opType must have (
          'type ("transformation"),
          'id ("id"),
          'name ("mapper1"),
          'op ("map"),
          'paramsEncoded (false)
        )
      }
    }
  }
}