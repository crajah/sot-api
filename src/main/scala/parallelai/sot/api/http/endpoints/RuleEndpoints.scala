package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import io.finch.sprayjson._
import io.finch.syntax.{Mapper => _, _}
import io.finch.{Error => _, Errors => _, Input => _, _}
import shapeless.HNil
import spray.json.lenses.JsonLenses._
import spray.json.{JsValue, _}
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{DagActions, RuleActions}
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.service.GetVersionImpl
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.json.JsonLens._
import parallelai.sot.api.model._
import parallelai.sot.api.services.VersionService

class RuleEndpoints(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with RuleActions with DagActions {
  this: DatastoreConfig =>

  val getVersion = new GetVersionImpl

  val rulePath: Endpoint[HNil] = api.path :: "rule"

  lazy val ruleEndpoints = buildRule :+: buildRegisteredVersionRule :+: buildDag :+: ruleStatus :+: launchRule :+: allRule

  /**
   * curl -v -X PUT http://localhost:8082/api/2/rule/build -H "Content-Type: application/json" -d '{ "name": "my-rule", "version": "2" }'
   */
  lazy val buildRule: Endpoint[Response] =
    put(rulePath :: "build" :: jsonBody[JsValue]) { ruleJson: JsValue =>
      val ruleId = uniqueId(ruleJson.extract[String]('id.?) getOrElse ruleJson.extract[String]('name))
      val version = ruleJson.extract[String]("version")

      buildRule(ruleJson.update('id ! set(ruleId)), ruleId, version).toTFuture
    }

  lazy val buildRegisteredVersionRule: Endpoint[Result[String]] =
    put(rulePath :: "build" :: "registered-version" :: jsonBody[JsValue]) { ruleJson: JsValue =>
      val ruleId = uniqueId(ruleJson.extract[String]('id.?) getOrElse ruleJson.extract[String]('name)) // TODO - Taken from above

      (ruleJson.asJsObject.getFields("version", "organisation") match {
        case Seq(JsString(version), JsString(organisation)) =>
          versionService.versions.get(organisation -> version).fold(Result[String](Left(Errors(s"Non existing version: $version")), Status.BadRequest).pure[Future]) { registeredVersion =>
            getVersion(registeredVersion).map {
              case Right(file) => Result(file.name, Status.Ok)
              case Left(error) => Result[String](Left(Errors(error)), Status.BadRequest)
            }
          }

        case _ =>
          Result[String](Left(Errors(s"Invalid JSON")), Status.BadRequest).pure[Future]

      }).toTFuture
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

object RuleEndpoints {
  def apply(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) =
    (new RuleEndpoints(versionService) with DatastoreConfig).ruleEndpoints
}