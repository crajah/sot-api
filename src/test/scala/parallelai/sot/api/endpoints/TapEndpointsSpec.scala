package parallelai.sot.api.endpoints

import scala.concurrent.Future
import io.finch.Input._
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore.{ FromEntity, ToEntity }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.common.persist.Identity
import parallelai.sot.api.actions.DagActions
import parallelai.sot.api.entities.TapWrapper
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition

class TapEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  implicit private val typeWrapperIdentity: Identity[TapWrapper] = Identity[TapWrapper](_.id)

  "Tap endpoints" should {
    "retrieve all taps" in new TapEndpoints with DatastoreConfig with DagActions {
      override lazy val tapDAO: ApiDatastore[TapWrapper] = new ApiDatastore[TapWrapper] {
        override def findAll[L <: HList](implicit gen: Aux[TapWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[TapWrapper]] =
          Future.successful(List(
            TapWrapper("id", PubSubTapDefinition("pubsub", "id", "topic", None, None, None)),
            TapWrapper("id2", PubSubTapDefinition("pubsub", "id2", "topic", None, None, None))))
      }

      val Some(response) = taps(get(p"/$tapPath")).awaitValueUnsafe()

      val pubSubs: List[PubSubTapDefinition] = response.content.convertTo[List[PubSubTapDefinition]]
      pubSubs.size mustEqual 2
      pubSubs.map(_.id) mustEqual List("id", "id2")
    }

    "retrieve a step for a given id" in new TapEndpoints with DatastoreConfig with DagActions {
      override lazy val tapDAO: ApiDatastore[TapWrapper] = new ApiDatastore[TapWrapper] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[TapWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[TapWrapper]] =
          Future.successful(Some(TapWrapper("id", PubSubTapDefinition("pubsub", "id", "topic", None, None, None))))
      }

      val Some(response) = tap(get(p"/$tapPath/id")).awaitValueUnsafe()

      response.content.convertTo[PubSubTapDefinition].id mustEqual "id"
    }

    "return an error message if tap cannot be found" in new TapEndpoints with DatastoreConfig {
      override lazy val tapDAO: ApiDatastore[TapWrapper] = new ApiDatastore[TapWrapper] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[TapWrapper, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[TapWrapper]] =
          Future.successful(None)
      }

      val Some(response) = tap(get(p"/$tapPath/anyId")).awaitValueUnsafe()

      response.content.compactPrint mustEqual """{"message":"Non existing tap: anyId - Cannot proceed."}"""
    }
  }
}