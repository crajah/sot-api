package parallelai.sot.api.endpoints

import scala.concurrent.Future
import io.finch.Input._
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore.{ FromEntity, ToEntity }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ MustMatchers, WordSpec }
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.DatastoreConfig

class EnvEndpointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Env endpoints" should {
    "retrieve all envs" in new EnvEndpoints with DatastoreConfig {
      override lazy val environmentDAO: ApiDatastore[Environment] = new ApiDatastore[Environment] {
        override def findAll[L <: HList](implicit gen: Aux[Environment, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[Environment]] =
          Future.successful(List(
            Environment("envId", "envName", EncryptedString("username_password"), "launchOpt", "projectId"),
            Environment("envId2", "envName2", EncryptedString("username_password"), "launchOpt2", "projectId")))
      }

      val Some(response) = environments(get(p"/$envPath")).awaitValueUnsafe()

      val result: List[Environment] = response.content.convertTo[List[Environment]]
      result.size mustEqual 2
      result.map(_.id) mustEqual List("envId", "envId2")
    }

    "retrieve an env for a given id" in new EnvEndpoints with DatastoreConfig {
      override lazy val environmentDAO: ApiDatastore[Environment] = new ApiDatastore[Environment] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[Environment, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[Environment]] =
          Future.successful(Some(Environment("envId", "envName", EncryptedString("username_password"), "launchOpt", "projectId")))
      }

      val Some(response) = environment(get(p"/$envPath/envId")).awaitValueUnsafe()

      response.content.convertTo[Environment].id mustEqual "envId"
    }
  }
}