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
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.model.IdGenerator99UniqueSuffix
import parallelai.sot.api.time.Time2016
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class DagEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures
                         with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                         with EndpointOps with DatastoreMappableType with EntityFormats {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val dagMapping = JsObject(ListMap(
    "name" -> JsString("dag-rule"),
    "edges" -> JsArray(
      JsObject(ListMap(
        "from" -> JsString("a"),
        "to" -> JsString("b")
      )),
      JsObject(ListMap(
        "from" -> JsString("x"),
        "to" -> JsString("y")
      ))
    )
  ))

  "Dag endpoints" should {
    "fail to register an invalid dag" in new DagEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val dagWithMissingEdges: JsValue = dagMapping - "edges"

      val throwable: Throwable = intercept[Throwable] {
        dagEndpoints(post(p"/$dagPath").withBody[Application.Json](dagWithMissingEdges)).awaitValueUnsafe()
      }

      throwable.getCause must (be (a [DeserializationException]) and have message "Invalid DAG - Missing edges")
    }

    "retrieve all dags" in new DagEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val Some(response) = registerDag(post(p"/$dagPath").withBody[Application.Json](dagMapping)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      val Some(findAllResponse) = dags(get(p"/$dagPath")).awaitValueUnsafe()

      findAllResponse.content.convertTo[List[Dag]] mustEqual List(Dag("dag-rule-99", "dag-rule", Edge("a", "b"), Edge("x", "y")))
    }

    "retrieve a dag for a given id" in new DagEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val Some(response) = registerDag(post(p"/$dagPath").withBody[Application.Json](dagMapping)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      val Some(findAllResponse) = dag(get(p"/$dagPath/dag-rule-99")).awaitValueUnsafe()

      findAllResponse.content.convertTo[Dag] mustEqual Dag("dag-rule-99", "dag-rule", Edge("a", "b"), Edge("x", "y"))
    }

    """create a new dag with name (that is used as prefix to an auto generated ID to act as the "rule ID")""" in new DagEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val Some(response) = registerDag(post(p"/$dagPath").withBody[Application.Json](dagMapping)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      response.content.convertTo[Dag] must have (
        'id ("dag-rule-99"),
        'name ("dag-rule"),
        'edges (Seq(Edge("a", "b"), Edge("x", "y")))
      )

      whenReady(dagDAO findOneById "dag-rule-99") { registeredDag =>
        registeredDag.get must have (
          'id ("dag-rule-99"),
          'edges (Seq(Edge("a", "b"), Edge("x", "y")))
        )
      }
    }

    """create a new dag without name so producing a "rule ID" that is solely auto generated""" in new DagEndpoints with IdGenerator99UniqueSuffix with Time2016 with DatastoreITConfig {
      val dagWithMissingName: JsValue = dagMapping - "name"

      val Some(response) = registerDag(post(p"/$dagPath").withBody[Application.Json](dagWithMissingName)).awaitValueUnsafe()
      response.status mustEqual Status.Ok

      response.content.convertTo[Dag] must have (
        'id ("99"),
        'name (""),
        'edges (Seq(Edge("a", "b"), Edge("x", "y")))
      )

      whenReady(dagDAO findOneById "99") { registeredDag =>
        registeredDag.get must have (
          'id ("99"),
          'edges (Seq(Edge("a", "b"), Edge("x", "y")))
        )
      }
    }
  }
}