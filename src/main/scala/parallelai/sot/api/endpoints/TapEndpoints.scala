package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import fommil.sjs.FamilyFormats._
import grizzled.slf4j.Logging
import io.finch._
import io.finch.sprayjson._
import io.finch.syntax._
import shapeless.HNil
import shapeless.datatype.datastore._
import spray.json._
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{ DagActions, Response }
import parallelai.sot.api.config._
import parallelai.sot.api.entities.{ Error, Id, _ }
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.executor.model.SOTMacroConfig._

trait TapEndpoints extends EndpointOps with DagActions with DatastoreMappableType with EntityFormats with Logging {
  this: DatastoreConfig =>

  val tapPath: Endpoint[HNil] = api.path :: "tap"

  val tapEndpoints = tap :+: taps :+: registerTap :+: deleteTap

  lazy val tap: Endpoint[Response] =
    get(tapPath :: path[String]) { id: String =>
      (tapDAO findOneById id).map {
        case Some(t) => Response(t.tap)
        case _ => Response(Error(s"Non existing tap: $id - Cannot proceed."), Status.NotFound)
      }.toTFuture
    }

  lazy val taps: Endpoint[Response] = get(tapPath) {
    tapDAO.findAll.map(taps => Response(taps.map(_.tap))).toTFuture
  }

  lazy val registerTap: Endpoint[Response] =
    post(tapPath :: jsonBody[TapDefinition]) { tap: TapDefinition =>
      info(s"Registering tap: $tap")
      tapDAO.insert(TapWrapper(tap.id, tap)).map(t => Response(t.tap)).toTFuture
    }

  lazy val deleteTap: Endpoint[Response] =
    delete(tapPath :: jsonBody[Id]) { id: Id =>
      tapDAO.deleteById(id.value).map(_ => Response(JsObject())).toTFuture
    }
}