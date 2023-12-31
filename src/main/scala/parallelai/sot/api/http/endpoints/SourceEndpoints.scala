package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore._
import spray.json._
import org.joda.time.Instant.now
import parallelai.sot.api.config._
import parallelai.sot.api.model._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.model.Id

@deprecated(message = "I believe this is no longer used", since = "2nd March 2018")
trait SourceEndpoints extends EndpointOps with DefaultJsonProtocol with DatastoreMappableType {
  this: DatastoreConfig =>

  // TODO - This should not be here
  lazy val sourceSinkDAO = datastore[SourceSink]

  val sourcePath: Endpoint[HNil] = api.path :: "source"

  val sourceEndpoints = source :+: createSource :+: deleteSource

  lazy val source: Endpoint[Response] =
    get(sourcePath :: paramOption("id")) { id: Option[String] =>
      def findAll = sourceSinkDAO.findAll.map(Response(_))

      def findAllById(id: String) = sourceSinkDAO.findAllById(id).map(Response(_))

      id.fold(findAll)(findAllById).toTFuture
    }

  lazy val createSource: Endpoint[Response] =
    post(sourcePath :: jsonBody[SourceSink]) { request: SourceSink =>
      sourceSinkDAO.put(request.copy(timestamp = Some(now))).map(Response(_)).toTFuture
    }

  lazy val deleteSource: Endpoint[Response] =
    delete(sourcePath :: jsonBody[Id]) { id: Id =>
      sourceSinkDAO.deleteById(id.value).map(_ => Response(JsObject())).toTFuture
    }
}

object SourceEndpoints {
  def apply() = (new SourceEndpoints with DatastoreConfig).sourceEndpoints
}