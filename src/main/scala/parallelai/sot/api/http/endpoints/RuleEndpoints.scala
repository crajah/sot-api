package parallelai.sot.api.http.endpoints

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import io.finch.sprayjson._
import io.finch.syntax.{Mapper => _, _}
import io.finch.{Error => _, Errors => _, Input => _, _}
import shapeless.HNil
import spray.json.lenses.JsonLenses._
import spray.json.{JsValue, _}
import com.github.nscala_time.time.Imports.DateTime
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{DagActions, RuleActions}
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.service.GetVersionImpl
import parallelai.sot.api.json.SprayJsonLens._
import parallelai.sot.api.mechanics.DOWNLOAD_DONE
import parallelai.sot.api.model._
import parallelai.sot.api.services.VersionService

class RuleEndpoints(versionService: VersionService)(implicit sb: SttpBackend[Future, Nothing]) extends EndpointOps with RuleActions with DagActions {
  this: DatastoreConfig =>

  val getVersion = new GetVersionImpl

  val rulePath: Endpoint[HNil] = api.path :: "rule"

  lazy val ruleEndpoints = buildRule :+: buildDag :+: ruleStatus :+: launchRule :+: allRule

  /////////////////////////// TESTING
  val token = Token("licenceId", "organisation", "me@gmail.com")
  val uri = new URI("https://www.googleapis.com/download/storage/v1/b/sot-rules/o/licenceId-parallelai-sot-v0-encrypted.zip?generation=1522091908107420&alt=media")
  val registeredVersion = RegisteredVersion(uri, "v0", token, DateTime.nextDay)

  versionService.versions += ("organisation", "v0") -> registeredVersion
  ///////////////////////////

  /**
   * curl -v -X PUT http://localhost:8082/api/2/rule/build -H "Content-Type: application/json" -d '{ "name": "my-rule", "version": "2" }'
   */
  lazy val buildRule: Endpoint[Response] =
    put(rulePath :: "build" :: jsonBody[JsValue]) { ruleJson: JsValue =>
      val ruleId = uniqueId(ruleJson.extract[String]('id.?) getOrElse ruleJson.extract[String]('name))

      (ruleJson.asJsObject.getFields("version", "organisation") match {
        case Seq(JsString(version), JsString(organisation)) =>
          println(s"===> NEW") // TODO

          versionService.versions.get(organisation -> version).fold(Response(Response.Error(s"Non existing version: $version"), Status.BadRequest).pure[Future]) { registeredVersion =>
            getVersion(registeredVersion).map {
              case Right(file) =>
                Response(RuleStatus(s"Rule ruleId: File ${file.name} downloaded", DOWNLOAD_DONE), Status.Ok)
                // TODO - decrypt, unzip and build

              case Left(error) =>
                Response(Response.Error(error), Status.BadRequest)
            }
          }

        case Seq(JsString(version)) =>
          println(s"===> OLD") // TODO
          buildRule(ruleJson.update('id ! set(ruleId)), ruleId, version)

        case _ =>
          Response(Response.Error(s"Invalid JSON"), Status.BadRequest).pure[Future]
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