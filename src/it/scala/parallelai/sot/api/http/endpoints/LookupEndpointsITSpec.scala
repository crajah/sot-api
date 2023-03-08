package parallelai.sot.api.http.endpoints

import io.finch.Application
import io.finch.Input._
import io.finch.sprayjson._
import shapeless.datatype.datastore.DatastoreMappableType
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Inside, MustMatchers, WordSpec}
import com.dimafeng.testcontainers.Container
import com.twitter.finagle.http.Status
import parallelai.sot.api.gcp.datastore.{DatastoreContainerFixture, DatastoreFixture}
import parallelai.sot.containers.ForAllContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture
import parallelai.sot.executor.model.SOTMacroConfig.{DatastoreLookupDefinition, LookupDefinition}

class LookupEndpointsITSpec extends WordSpec with MustMatchers with Inside with ScalaFutures
                            with ForAllContainersFixture with ProjectFixture with DatastoreContainerFixture with DatastoreFixture
                            with EndpointOps with DatastoreMappableType  {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(20, Millis))

  val container: Container = datastoreContainer

  val mockLookup = Map("schema" -> "avroschema1", "tap" -> "datastore1")

  "Lookup endpoints" should {
    "return all lookups from datastore if any" in new LookupEndpoints with DatastoreITConfig {
      val lookup = DatastoreLookupDefinition("id", "avroSchema1", "datastore1")
      registerLookup(post(p"/$lookupPath").withBody[Application.Json](lookup)).awaitValueUnsafe()

      val Some(response) = lookups(get(p"/$lookupPath")).awaitValueUnsafe()

      response.status mustEqual Status.Ok

      response.content.convertTo[Seq[LookupDefinition]] must matchPattern {
        case Seq(DatastoreLookupDefinition(_, "avroSchema1", "datastore1")) =>
      }
    }

    "create a new Lookup" in new LookupEndpoints with DatastoreITConfig {
      val lookup = DatastoreLookupDefinition("some-id", "avroSchema1", "datastore1")
      val Some(response) = registerLookup(post(p"/$lookupPath").withBody[Application.Json](lookup)).awaitValueUnsafe()

      response.status mustEqual Status.Ok

      whenReady(lookupDAO findAll) { registeredLookup =>
        registeredLookup must matchPattern {
          case Seq(DatastoreLookupDefinition(_, "avroSchema1", "datastore1")) =>
        }
      }
    }
  }
}