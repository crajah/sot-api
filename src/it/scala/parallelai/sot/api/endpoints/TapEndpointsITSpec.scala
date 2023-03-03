package parallelai.sot.api.endpoints

import scala.collection.immutable.ListMap
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore._
import spray.json.{JsObject, JsString, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.twitter.finagle.http.Status
import parallelai.sot.api.entities.EntityFormats
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.model.IdGenerator99UniqueSuffix
import parallelai.sot.api.time.Time2016
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture
import parallelai.sot.executor.model.SOTMacroConfig._

class TapEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures
                         with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                         with EndpointOps with DatastoreMappableType with EntityFormats {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val pubSubTap = JsObject(ListMap(
    "type" -> JsString("pubsub"),
    "id" -> JsString("pubsubsource1"),
    "topic" -> JsString("p2pin"),
    "managedSubscription" -> JsBoolean(false),
    "timestampAttribute" -> JsString(""),
    "idAttribute" -> JsString("")
  ))

  "Tap endpoints" should {
    "fail to register tap upon receiving an unrecognised tap" in new TapEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val pubSubTapWithMissingType: JsValue = pubSubTap - "type"

      val throwable: Throwable = intercept[Throwable] {
        tapEndpoints(post(p"/$tapPath").withBody[Application.Json](pubSubTapWithMissingType)).awaitValueUnsafe()
      }

      throwable.getCause must (be (a [DeserializationException]) and have message "Source expected")
    }

    "fail to register tap upon receiving an invalid tap definition" in new TapEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val pubSubTapMissingTopic: JsValue = pubSubTap - "topic"

      val throwable: Throwable = intercept[Throwable] {
        tapEndpoints(post(p"/$tapPath").withBody[Application.Json](pubSubTapMissingTopic)).awaitValueUnsafe()
      }

      throwable.getCause must (be (a [DeserializationException]) and have message "Pubsub source expected")
    }

    "create a new tap with an ID" in new TapEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val Some(response) = registerTap(post(p"/$tapPath").withBody[Application.Json](pubSubTap)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      response.content.convertTo[TapDefinition] must have (
        'type ("pubsub"),
        'id ("pubsubsource1"),
        'topic ("p2pin")
      )

      whenReady(tapDAO findOneById "pubsubsource1") { registeredTapWrapper =>
        registeredTapWrapper.map(_.tap).get must have (
          'type ("pubsub"),
          'id ("pubsubsource1"),
          'topic ("p2pin")
        )
      }
    }

    "create a new tap without an ID (unlike most, we shall make the ID unique by taking the given topic as no name is provided)" in new TapEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val pubSubTapWithoutId: JsValue = pubSubTap - "id"

      val Some(response) = registerTap(post(p"/$tapPath").withBody[Application.Json](pubSubTapWithoutId)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      response.content.convertTo[TapDefinition] must have (
        'type ("pubsub"),
        'id ("p2pin-99"),
        'topic ("p2pin")
      )

      whenReady(tapDAO findOneById "p2pin-99") { registeredTapWrapper =>
        registeredTapWrapper.map(_.tap).get must have (
          'type ("pubsub"),
          'id ("p2pin-99"),
          'topic ("p2pin")
        )
      }
    }
  }
}