package parallelai.sot.api.endpoints

import scala.concurrent.Future
import io.finch.Input._
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore.{ FromEntity, ToEntity }
import spray.json.CollectionFormats
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.executor.model.SOTMacroConfig.{ OpType, SourceOp }

class StepEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures with CollectionFormats {
  "Step endpoints" should {
    "retrieve all steps" in new StepEndpoints with DatastoreConfig {
      override lazy val stepDAO: ApiDatastore[OpTypeWrapper] = new ApiDatastore[OpTypeWrapper] {
        override def findAll[L <: HList](implicit gen: Aux[OpTypeWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[OpTypeWrapper]] =
          Future.successful(List(
            OpTypeWrapper("id", SourceOp("source", "id", "name", "schema", "tap")),
            OpTypeWrapper("id2", SourceOp("source", "id2", "name", "schema", "tap"))))
      }

      val Some(response) = steps(get(p"/$stepPath")).awaitValueUnsafe()

      val opTypes: Seq[OpType] = response.content.convertTo[Seq[OpType]]
      opTypes.size mustEqual 2
      opTypes.map(_.id) mustEqual List("id", "id2")
    }

    "retrieve a step for a given id" in new StepEndpoints with DatastoreConfig {
      override lazy val stepDAO: ApiDatastore[OpTypeWrapper] = new ApiDatastore[OpTypeWrapper] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[OpTypeWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[OpTypeWrapper]] =
          Future.successful(Some(OpTypeWrapper("id", SourceOp("source", "id", "name", "schema", "tap"))))
      }

      val Some(response) = step(get(p"/$stepPath/id")).awaitValueUnsafe()

      response.content.convertTo[OpType].id mustEqual "id"
    }

    "return an error message if step cannot be found" in new StepEndpoints with DatastoreConfig {
      override lazy val stepDAO: ApiDatastore[OpTypeWrapper] = new ApiDatastore[OpTypeWrapper] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[OpTypeWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[OpTypeWrapper]] =
          Future.successful(None)
      }

      val Some(response) = step(get(p"/$stepPath/anyId")).awaitValueUnsafe()

      response.content.compactPrint mustEqual """{"error-message":"Non existing step: anyId - Cannot proceed."}"""
    }
  }
}