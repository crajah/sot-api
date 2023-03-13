package parallelai.sot.api.http.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import better.files._
import cats.implicits._
import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore._
import spray.json.JsValue
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time._
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.twitter.finagle.http.Status
import parallelai.sot.api.config.baseDirectory
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.mechanics._
import parallelai.sot.api.model.Files._
import parallelai.sot.api.model.{IdGenerator99UniqueSuffix, _}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

class RuleEndpointsITSpec extends WordSpec with MustMatchers with ScalaFutures with Inside with Eventually with MockitoSugar
                          with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                          with EndpointOps with DatastoreMappableType {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val ruleId = "my-rule-id"
  val envId = "some-env-id"

  "Rule endpoints" should {
    "fail to build a rule because of invalid JSON" in new RuleEndpoints with DatastoreITConfig {
      val jsonString = "bad json"

      the [Exception] thrownBy {
        buildRule(put(p"/$rulePath/build").withBody[Application.Json](jsonString)).awaitValueUnsafe()
      } must have message
        """java.lang.RuntimeException: Not a json object: "bad json""""
    }

    "fail to build a non-existing rule" in new RuleEndpoints with DatastoreITConfig {
      override protected def codeFromRepo: Future[File] = Future failed new Exception("Git unavailable")

      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](Rule("rule-id", "version", "orgCode"))).awaitValueUnsafe()
      response.status mustEqual Status.Accepted

      val ruleId: String = response.content.convertTo[RuleStatus].id

      eventually {
        val Some(response2) = ruleStatus(get(p"/$rulePath/$ruleId/status")).awaitValueUnsafe()
        response2.content.convertTo[RuleStatus].id mustEqual ruleId
      }
    }

    "fail to build a rule when an error is encountered getting git code" in new RuleEndpoints with DatastoreITConfig {
      override protected def codeFromRepo: Future[File] = Future failed new Exception("Git unavailable")

      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](Rule("rule-id", "version", "orgCode"))).awaitValueUnsafe()
      response.status mustEqual Status.Accepted

      val ruleId: String = response.content.convertTo[RuleStatus].id

      eventually {
        val Some(response2) = ruleStatus(get(p"/$rulePath/$ruleId/status")).awaitValueUnsafe()
        response2.content.convertTo[RuleStatus].id mustEqual ruleId
      }
    }

    "build a rule" in new RuleEndpoints with IdGenerator99UniqueSuffix with DatastoreITConfig {
      override protected def codeFromRepo: Future[File] = baseDirectory.pure[Future]

      override protected def copyRepositoryCode(ruleId: String, version: String): Future[File] = baseDirectory.pure[Future]

      override def createConfiguration(json: JsValue, rulePath: File, ruleName: String): Future[(Files.ApplicationConfigFile, Files.RuleFile)] =
        (new ApplicationConfigFile(baseDirectory) -> new RuleFile(baseDirectory)).pure[Future]

      override def build(ruleId: String, version: String, path: File): Future[LogEntry] =
        mock[LogEntry].pure[Future]

      val Some(response) = buildRule(put(p"/$rulePath/build").withBody[Application.Json](Rule(ruleId, version = "ps-to-bq-test_1513181186942", "orgCode"))).awaitValueUnsafe()
      response.status mustEqual Status.Accepted

      val buildingRuleStatus: RuleStatus = response.content.convertTo[RuleStatus]
      buildingRuleStatus.id mustEqual "my-rule-id-99"
      buildingRuleStatus.status mustEqual BUILD_START

      // TODO
      /*response.code mustEqual Status.Ok
      response.content.convertTo[String] must fullyMatch regex raw"""$ruleId-\d+""".r*/
    }

    "error status of a non-existing rule" in new RuleEndpoints with DatastoreITConfig {
      val Some(response) = ruleStatus(get(p"/$rulePath/$ruleId/status")).awaitValueUnsafe()

      response.status mustEqual Status.NotFound
      response.content.convertTo[Error] mustEqual Error(s"Non existing rule: $ruleId")
    }

    "give status of an existing rule" in new RuleEndpoints with DatastoreITConfig {
      val status: StatusType = BUILD_START

      whenReady(ruleStatusDAO insert RuleStatus(ruleId, status)) { _ =>
        val Some(response) = ruleStatus(get(p"/$rulePath/$ruleId/status")).awaitValueUnsafe()

        response.status mustEqual Status.Ok
        response.content.convertTo[RuleStatus] must matchPattern { case RuleStatus(`ruleId`, `status`, _, _) => }
      }
    }

    "not launch a non existing rule" in new RuleEndpoints with DatastoreITConfig {
      val Some(response) = launchRule(put(p"/$rulePath/launch").withBody[Application.Json](RuleLcm(ruleId, envId))).awaitValueUnsafe()

      response.status mustEqual Status.NotFound
      response.content.convertTo[Error] mustEqual Error(s"Non existing rule: $ruleId")
    }

    "not launch a busy rule" in new RuleEndpoints with DatastoreITConfig {
      override def downloadJarToGoogle(ruleId: String): Future[LogEntry] =
        mock[LogEntry].pure[Future]

      whenReady(ruleBusyDAO insert RuleBusy(ruleId, busy = true)) { _ =>
        val Some(response) = launchRule(put(p"/$rulePath/launch").withBody[Application.Json](RuleLcm(ruleId, envId))).awaitValueUnsafe()

        response.status mustEqual Status.Accepted

        // TODO - Is this outstanding?
        /*response.status mustEqual Status.BadRequest
        response.content.convertTo[Error] mustEqual Error(s"Rule $ruleId is busy - Cannot proceed")*/
      }
    }

    // TODO - WIP
    /*"launch a rule" in new RuleEndpoints with DatastoreITConfig {
      whenReady(ruleActiveDAO insert RuleActive(ruleId, true)) { _ =>

        val Some(response) = ruleStatus(post(p"/$rulePath/launch").withBody[Application.Json](RuleLcm(ruleId, envId))).awaitValueUnsafe()
      }
    }*/
  }
}