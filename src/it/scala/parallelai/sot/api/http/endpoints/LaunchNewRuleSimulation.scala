package parallelai.sot.api.http.endpoints

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import spray.json._
import parallelai.sot.api.model.Version
import parallelai.sot.api.text.StringOps
import parallelai.sot.api.config.licence

class LaunchNewRuleSimulation extends Simulation with StringOps {
  // TODO - Remove hardcoded URI
  val httpConf: HttpProtocolBuilder = http
    .baseURL(s"http://127.0.0.1:8881/${licence.name}/2")
    .contentTypeHeader("application/json")
    .acceptHeader("application/json")
    .doNotTrackHeader("1")
    .disableResponseChunksDiscarding // TODO - Make configurable especially for performance test as it can slows things down
    .extraInfoExtractor { extraInfo =>
      println(s"Request:\n${extraInfo.request}\n")
      println(s"Response:\n${extraInfo.response.body.string.parseJson.prettyPrint}\n")
      Nil
    }

  val versions: ChainBuilder = exec(http("Versions").get("/version"))

  val version = Version("v0.1.4")
  val versionCreate: ChainBuilder = exec(http("Version creation").post("/version?wait=true").body(StringBody(version.toJson.prettyPrint)).asJSON)

  //val

  val scenarioBuilder: ScenarioBuilder = scenario(getClass.getSimpleName.title).exec(versions, versionCreate)

  setUp(
    scenarioBuilder.inject(atOnceUsers(1))
  ).protocols(httpConf)
}