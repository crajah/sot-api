package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.OptionT
import cats.implicits._
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore.DatastoreMappableType
import spray.json._
import org.joda.time.Instant.now
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.Response
import parallelai.sot.api.entities.{ Error, _ }
import parallelai.sot.api.gcp.datastore.DatastoreConfig

trait FolderEndpoints extends BasePath with EndpointOps with DefaultJsonProtocol with DatastoreMappableType {
  this: DatastoreConfig =>

  // TODO - This should not be here
  lazy val folderDAO = datastore[Folder]

  val folderPath: Endpoint[HNil] = basePath :: "folder"

  val folderEndpoints = folder :+: folders :+: createFolder :+: deleteFolder

  lazy val folder: Endpoint[Response] =
    get(folderPath :: path[String]) { id: String =>
      OptionT(folderDAO.findOneById(id))
        .fold(Response(Error(s"Non existing folder: $id - Cannot proceed."), Status.NotFound))(folder => Response(folder)).toTFuture
    }

  lazy val folders: Endpoint[Response] =
    get(folderPath) {
      folderDAO.findAll.map(Response(_)).toTFuture
    }

  lazy val createFolder: Endpoint[Response] =
    post(folderPath :: jsonBody[Folder]) { folder: Folder =>
      folderDAO.put(folder.copy(timestamp = Some(now))).map(Response(_)).toTFuture
    }

  lazy val deleteFolder: Endpoint[Response] =
    delete(folderPath :: jsonBody[Id]) { id: Id =>
      folderDAO.deleteById(id.value).map(_ => Response(JsObject())).toTFuture
    }
}