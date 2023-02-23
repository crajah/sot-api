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

class FolderEndPointsSpec extends WordSpec with MustMatchers with ScalaFutures {
  "Folder endpoints" should {
    "retrieve all folders" in new FolderEndpoints with DatastoreConfig {
      override lazy val folderDAO: ApiDatastore[Folder] = new ApiDatastore[Folder] {
        override def findAll[L <: HList](implicit gen: Aux[Folder, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[Folder]] =
          Future.successful(List(
            Folder("id", "name", "parent", Nil, Nil, Nil, Nil, Nil),
            Folder("id2", "name2", "parent2", Nil, Nil, Nil, Nil, Nil)))
      }

      val Some(response) = folders(get(p"/$folderPath")).awaitValueUnsafe()

      val result: List[Folder] = response.content.convertTo[List[Folder]]
      result.size mustEqual 2
      result.map(_.id) mustEqual List("id", "id2")
    }

    "retrieve a folder for a given id" in new FolderEndpoints with DatastoreConfig {
      override lazy val folderDAO: ApiDatastore[Folder] = new ApiDatastore[Folder] {
        override def findOneById[L <: HList](id: String)(implicit gen: Aux[Folder, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[Folder]] =
          Future.successful(Some(Folder("id", "name", "parent", Nil, Nil, Nil, Nil, Nil)))
      }

      val Some(response) = folder(get(p"/$folderPath/envId")).awaitValueUnsafe()

      response.content.convertTo[Folder].id mustEqual "id"
    }
  }
}