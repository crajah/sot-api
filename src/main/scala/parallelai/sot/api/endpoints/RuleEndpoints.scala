package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import io.finch.sprayjson._
import io.finch.syntax.{Mapper => _, _}
import io.finch.{Error => _, Input => _, _}
import shapeless.HNil
import spray.json.lenses.JsonLenses._
import spray.json.{JsValue, _}
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{DagActions, RuleActions}
import parallelai.sot.api.config._
import parallelai.sot.api.entities._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.json.JsonLens._

trait RuleEndpoints extends EndpointOps with RuleActions with DagActions {
  this: DatastoreConfig =>

  val rulePath: Endpoint[HNil] = api.path :: "rule"

  lazy val ruleEndpoints = buildRule :+: buildDag :+: ruleStatus :+: launchRule :+: allRule

  /**
   * curl -v -X PUT http://localhost:8082/api/2/rule/build -H "Content-Type: application/json" -d '{ "name": "my-rule", "version": "2" }'
   */
  lazy val buildRule: Endpoint[Response] =
    put(rulePath :: "build" :: jsonBody[JsValue]) { ruleJson: JsValue =>
      val ruleId = uniqueId(ruleJson.extract[String]('id.?) getOrElse ruleJson.extract[String]('name))
      val version = ruleJson.extract[String]("version")

      buildRule(ruleJson.update('id ! set(ruleId)), ruleId, version).toTFuture
    }

  /**
   * curl -v -X PUT http://localhost:8082/api/2/rule/compose -H "Content-Type: application/json" -d '{ "id": "my-dag", "version": "2" }'
   */
  lazy val buildDag: Endpoint[Response] =
    put(rulePath :: "compose" :: jsonBody[JsValue]) { dagJson: JsValue =>
      val id = dagJson.extract[String]('id)
      val version = dagJson.extract[String]('version)

      buildDag(id).flatMap {
        case Left(error) =>
          Response(error, Status.BadRequest).pure[Future]

        case Right(ruleComposites) =>
          val json = ruleComposites.toJson << ("id", JsString(id)) << ("name", JsString(id)) << ("version", JsString(version)) // TODO - "name" is currently just taken from "id"
          buildRule(json, id, version).map(res => res.copy(res.content << ("rule", json)))
      }.toTFuture
    }

  /**
   * curl -v -X GET "http://localhost:8082/api/2/rule/my-rule/status"
   */
  lazy val ruleStatus: Endpoint[Response] =
    get(rulePath :: path[String] :: "status") { ruleId: String =>
      status(ruleId).toTFuture
    }

  lazy val launchRule: Endpoint[Response] =
    put(rulePath :: "launch" :: jsonBody[RuleLcm]) { ruleLcm: RuleLcm =>
      launchRule(ruleLcm).toTFuture
    }

  lazy val allRule: Endpoint[Response] = get(rulePath :: paramOption("all") :: paramOption("latest")) { (all: Option[String], latest: Option[String]) =>
    jobs(all, latest).toTFuture
  }
}