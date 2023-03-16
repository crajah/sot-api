package parallelai.sot.api.actions

import java.io
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import shapeless.datatype.datastore._
import spray.json._
import com.google.api.services.dataflow.model
import com.softwaremill.sttp.{Id, _}
import com.twitter.finagle.http.Status
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.{Errors, Result}
import parallelai.sot.api.http.endpoints.Response
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.http.service.GoogleCloudService
import parallelai.sot.api.mechanics.GoogleJobStatus._
import parallelai.sot.api.mechanics._
import parallelai.sot.api.model.Job._
import parallelai.sot.api.model.{IdGenerator, _}
import better.files._

trait RuleActions extends EntityFormats with DatastoreMappableType with IdGenerator
  with GitMechanic with SbtMechanic with ConfigMechanic with StatusMechanic
  with LaunchMechanic with GoogleStorageMechanic with DataflowMechanic with GoogleCloudService {

  this: DatastoreConfig =>

  def buildRule(ruleJson: JsValue, ruleId: String, version: String): Future[Response] = {
    debug(s"Received following rule to build:\n${ruleJson.prettyPrint}")

    // TODO - Validate the Config object

    for {
      _ <- changeStatus(ruleId, START)
      // TODO - Implement validation of the rule schema
      _ <- busy(ruleId, changeStatus(ruleId, VALIDATE_DONE))
      _ <- codeFromRepo
      ruleDirectory <- copyRepositoryCode(ruleId, version) // TODO Tag parameters to not confuse as they are both Strings
      _ <- setRuleInfo(ruleId, version, None, None, ruleDirectory)
      _ <- changeStatus(ruleId, CODE_DONE)
      _ <- createConfiguration(ruleJson, ruleDirectory, ruleId)
      _ <- changeStatus(ruleId, CONFIG_DONE)
    } yield build(ruleId, version, ruleDirectory)

    Response(RuleStatus(ruleId, BUILD_START), Status.Accepted).pure[Future]
  }

  def buildRule(registeredVersion: RegisteredVersion)(implicit sb: SttpBackend[Future, Nothing]): Future[Response] = {
    val request: RequestT[Id, io.File, Nothing] /*: Request[Result[RegisteredProduct], Nothing]*/ =
      sttp get uri"${registeredVersion.uri.toString}" response asFile((baseDirectory / "temp").createIfNotExists(createParents = true).toJava)

    request.send.map { response =>
      println(response) // Response[File]

      /*response.body match {
        case Right(result @ Result(Right(registeredProduct), _)) =>
          licenceService.licenceId = registeredProduct.apiSharedSecret.decrypt.id
          licenceService.apiSharedSecret = createClientSharedSecret(registeredProduct.serverPublicKey)

          result

        case Right(result @ Result(Left(errors), status)) =>
          result

        case Left(error) =>
          Result(Left(Errors(error)), Status.UnprocessableEntity)
      }*/
    }





    findCryptFile(registeredVersion.uri)
    Response(RuleStatus(registeredVersion.token.code, BUILD_START), Status.Accepted).pure[Future]
  }

  def status(ruleId: String): Future[Response] =
    ruleStatusDAO findOneById ruleId map {
      case Some(ruleStatus) => Response(ruleStatus)
      case _ => Response(Error(s"Non existing rule: $ruleId"), Status.NotFound)
    }

  // TODO - Use the following (instead of the one above)
  /*def status(ruleId: String): Future[Response] = ruleStatusDAO findOneById ruleId map {
    case Some(ruleStatus) => ruleStatus.status match {
      case LAUNCH_DONE =>
        ruleStatus.envId.map { envId =>
          envDAO findOneById envId flatMap {
            case Some(env) => getJob(env.projectId, ruleId).map { job =>
              Response(ruleStatus.copy(status = StatusType.fromName(job.getCurrentState).get))
            }
          }
        }

      case _ =>
        Response(ruleStatus)
    }

    case _ =>
      Response(Error(s"Non existing rule: $ruleId"), Status.NotFound)
  }*/

  def jobs(allFlag: Option[String], latestFlag: Option[String]): Future[Response] = for {
    dsJobs <- ruleStatusDAO.findAll
    dfJobs <- googleRepo.findAllJobsByProjectId(executor.google.projectId)
    jobs = dfJobs.filter(job => dsJobs.map(_.id).contains(job.getName))
    runningJobs = jobs.filter(_.getCurrentState == JOB_STATE_RUNNING)
  } yield Response(allFlag.fold(runningJobs)(_ => latestFlag.fold(jobs)(_ => sortedByCreateTime(jobs))).map(_.toSotJob), Status.Ok)

  def launchRule(ruleLcm: RuleLcm): Future[Response] = {
    val ruleId = ruleLcm.id

    ruleBusyDAO findOneById ruleId flatMap {
      case None =>
        // TODO - Remove hack to check that rule exists
        Response(Error(s"Non existing rule: $ruleId"), Status.NotFound).pure[Future]

      case _ =>
        // TODO - Clean up
        for {
          _ <- whenNotBusy(ruleId, busy(ruleId, downloadJarToGoogle(ruleId)))
          result <- whenBusy(ruleId, launch(ruleLcm))
        } yield result match {
          case Left(e) => Response(e, Status.BadRequest)
          case Right(logEntry) => Response(logEntry)
        }

        Response(RuleStatus(ruleId, LAUNCH_START), Status.Accepted).pure[Future]
    }
  }

  private def sortedByCreateTime(jobs: Seq[model.Job]) =
    jobs.sortWith((t1, t2) => t1.getCreateTime > t2.getCreateTime)
}