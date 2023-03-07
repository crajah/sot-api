package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore._
import spray.json._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.twitter.finagle.http.Status
import parallelai.sot.api.model._
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture
import parallelai.sot.executor.model.SOTMacroConfig._

class RuleDagEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures with Inside with Eventually with MockitoSugar
                             with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                             with EndpointOps with DatastoreMappableType {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val dagId = "dag-id"
  val version = "v1.0.0"
  val dagToCompose = JsObject("id" -> JsString(dagId), "version" -> JsString(version))

  "Rule endpoints for DAG" should {
    "fail to build rule from DAG for a non-existing DAG" in new RuleEndpoints with DatastoreITConfig {
      val Some(response) = buildDag(put(p"/$rulePath/compose").withBody[Application.Json](dagToCompose)).awaitValueUnsafe()

      response.status mustEqual Status.BadRequest
      response.content.convertTo[Error] mustEqual Error(s"Invalid DAG ID $dagId")
    }

    "fail to build rule from DAG as there is only a schema" in new RuleEndpoints with DatastoreITConfig {
      val avroSchema = AvroSchema("avro", "avroSchemaFrom", "avroName", "v1.0.0", AvroDefinition("record", "avroName", "namespace", JsArray()))
      val dag = Dag(dagId, "dag-name", Edge("avroSchemaFrom", "avroSchemaTo"))

      whenReady {
        for {
          _ <- schemaDAO insert SchemaWrapper(avroSchema.id, avroSchema)
          _ <- dagDAO insert dag
        } yield ()
      } { _ =>
        val Some(response) = buildDag(put(p"/$rulePath/compose").withBody[Application.Json](dagToCompose)).awaitValueUnsafe()

        response.status mustEqual Status.BadRequest
        response.content.convertTo[Error] mustEqual Error("DAG dag-name (ID dag-id) has no associated Taps; Steps")
      }
    }

    "build rule from DAG" in new RuleEndpoints with DatastoreITConfig {
      val avroSchema = AvroSchema("avro", "avroSchemaFrom", "avroName", "v1.0.0", AvroDefinition("record", "avroName", "namespace", JsArray()))
      val pubSubTap = PubSubTapDefinition("pubsub", "pubSubFrom", "topic", None, None, None)
      val transformationOp = TransformationOp("transformation", "opId", "name", "map", Nil, paramsEncoded = false)
      val dag = Dag(dagId, "dag-name", Edge("avroSchemaFrom", "avroSchemaTo"), Edge("pubSubFrom", "pubSubTo"), Edge("opId", "opId"))

      whenReady {
        for {
          _ <- schemaDAO insert SchemaWrapper(avroSchema.id, avroSchema)
          _ <- tapDAO insert TapWrapper(pubSubTap.id, pubSubTap)
          _ <- opTypeDAO insert OpTypeWrapper(transformationOp.id, transformationOp)
          _ <- dagDAO insert dag
        } yield ()
      } { _ =>
        val Some(response) = buildDag(put(p"/$rulePath/compose").withBody[Application.Json](dagToCompose)).awaitValueUnsafe()

        response.status mustEqual Status.Accepted

        response.status mustEqual Status.Accepted
        response.content.convertTo[RuleComposites] must matchPattern { case _: RuleComposites => }
      }
    }
  }
}