package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files._
import cats.implicits._
import io.finch.sprayjson._
import io.finch.syntax.{Mapper => _, _}
import io.finch.{Error => _, Errors => _, Input => _, _}
import shapeless.HNil
import spray.json.lenses.JsonLenses._
import spray.json.{JsValue, _}
import com.softwaremill.sttp.SttpBackend
import com.twitter.finagle.http.Status
import parallelai.common.secure.{AES, Crypto, Encrypted}
import parallelai.sot.api.actions.{DagActions, RuleActions}
import parallelai.sot.api.config._
import parallelai.sot.api.file.GCFileNameConverter._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.service.GetVersionImpl
import parallelai.sot.api.json.SprayJsonLens._
import parallelai.sot.api.model._
import parallelai.sot.api.services.{LicenceService, VersionService}

class RuleEndpoints(implicit licenceService: LicenceService, versionService: VersionService, sb: SttpBackend[Future, Nothing]) extends EndpointOps with RuleActions with DagActions {
  this: DatastoreConfig =>

  val getVersion = new GetVersionImpl

  val rulePath: Endpoint[HNil] = api.path :: "rule"

  lazy val ruleEndpoints = buildRule :+: buildDag :+: ruleStatus :+: launchRule :+: allRule

  /**
   * curl -v -X PUT http://localhost:8082/api/2/rule/build -H "Content-Type: application/json" -d '{ "name": "my-rule", "version": "2" }'
   */
  lazy val buildRule: Endpoint[Response] =
    put(rulePath :: "build" :: paramOption("registered") :: paramOption("wait") :: jsonBody[JsValue]) { (registered: Option[String], wait: Option[String], ruleJson: JsValue) =>
      val ruleId: String = uniqueId(ruleJson.extract[String]('id.?) getOrElse ruleJson.extract[String]('name))
      val version: String = ruleJson.extract[String]("version")
      val organisation: Option[String] = ruleJson.extract[String]('organisation.?)

      val waitForBuild = wait.exists(w => w.isEmpty || w.equalsIgnoreCase("true"))

      ((registered, organisation) match {
        case (Some(reg), Some(org)) if reg.isEmpty || reg.equalsIgnoreCase("true") =>
          versionService.versions.get(org -> version).fold(Response(Response.Error(s"Non existing version: $version"), Status.BadRequest).pure[Future]) { registeredVersion =>
            getVersion(registeredVersion).flatMap {
              case Right(file) =>
                val crypto = Crypto(AES, licenceService.orgSharedSecret.getEncoded)
                val decryptedFile = (executor.directory / registeredVersion.defineFileName) writeByteArray Encrypted.fromBytes[Array[Byte]](file.byteArray).decrypt(crypto)

                decryptedFile.unzipTo(File(executor.rule.git.localPath) / ruleId / registeredVersion.version)

                buildRule(ruleJson.update('id ! set(ruleId)), ruleId, registeredVersion.version, registered = true, wait = waitForBuild)

              case Left(error) =>
                Response(Response.Error(error), Status.BadRequest).pure[Future]
            }
          }

        case _ =>
          buildRule(ruleJson.update('id ! set(ruleId)), ruleId, version, wait = waitForBuild)
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
  def apply(implicit licenceService: LicenceService, versionService: VersionService, sb: SttpBackend[Future, Nothing]) =
    new RuleEndpoints with DatastoreConfig ruleEndpoints
}