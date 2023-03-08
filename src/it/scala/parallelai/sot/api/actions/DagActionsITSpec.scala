package parallelai.sot.api.actions

import scala.concurrent.ExecutionContext.Implicits.global
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json.JsArray
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.model._
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture
import parallelai.sot.executor.model.SOTMacroConfig.{AvroDefinition, AvroSchema, PubSubTapDefinition, TransformationOp}

class DagActionsITSpec extends WordSpec with MustMatchers with ScalaFutures with Eventually with DatastoreMappableType
                       with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  "Dag actions" should {
    "fail to collect all required components" in new DagActions with DatastoreITConfig {
      val dag = Dag("dag-id", "dag-name")

      whenReady(ruleComposites(dag)) {
        _ mustBe Left(Error(s"DAG ${dag.name} (ID ${dag.id}) has no associated Schemas; Taps; Steps"))
      }
    }

    "fail to collect any Schemas" in new DagActions with DatastoreITConfig {
      val dag = Dag("dag-id", "dag-name", Edge("pubSubFrom", "pubSubTo"))

      whenReady {
        for {
          _ <- tapDAO insert TapWrapper("pubSubFrom", PubSubTapDefinition("pubsub", "pubSubFrom", "topic", None, None, None))
          rs <- ruleComposites(dag)
        } yield rs
      } { _ mustBe Left(Error(s"DAG ${dag.name} (ID ${dag.id}) has no associated Schemas; Steps")) }
    }

    "fail to collect any Taps" in new DagActions with DatastoreITConfig {
      val dag = Dag("dag-id", "dag-name", Edge("avroSchemaFrom", "avroSchemaTo"))

      whenReady {
        for {
          _ <- schemaDAO insert SchemaWrapper("avroSchemaFrom", AvroSchema("avro", "avroSchemaFrom", "avroName", "v1.0.0", AvroDefinition("record", "avroName", "namespace", JsArray())))
          rs <- ruleComposites(dag)
        } yield rs
      } { _ mustBe Left(Error(s"DAG ${dag.name} (ID ${dag.id}) has no associated Taps; Steps")) }
    }

    "acquire all necessary rule components that need to be built" in new DagActions with DatastoreITConfig {
      val dag = Dag("dag-id", "dag-name", Edge("avroSchemaFrom", "avroSchemaTo"), Edge("pubSubFrom", "pubSubTo"), Edge("opId", "opId"))

      val avroSchema = AvroSchema("avro", "avroSchemaFrom", "avroName", "v1.0.0", AvroDefinition("record", "avroName", "namespace", JsArray()))
      val pubSubTap = PubSubTapDefinition("pubsub", "pubSubFrom", "topic", None, None, None)
      val transformationOp = TransformationOp("transformation", "opId", "name", "map", Nil, paramsEncoded = false)

      whenReady {
        for {
          _ <- schemaDAO insert SchemaWrapper(avroSchema.id, avroSchema)
          _ <- tapDAO insert TapWrapper(pubSubTap.id, pubSubTap)
          _ <- opTypeDAO insert OpTypeWrapper(transformationOp.id, transformationOp)
          rs <- ruleComposites(dag)
        } yield rs
      } { _ mustBe Right(RuleComposites(schemas = Seq(avroSchema), taps = Seq(pubSubTap), opTypes = Seq(transformationOp), dag = dag.edges)) }
    }
  }
}