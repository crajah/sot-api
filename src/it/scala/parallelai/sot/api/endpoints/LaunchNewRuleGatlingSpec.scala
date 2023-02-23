package parallelai.sot.api.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.funspec.GatlingHttpFunSpec
import io.gatling.http.protocol.HttpProtocolBuilder
import spray.json._
import parallelai.sot.api.text.StringOps

class LaunchNewRuleGatlingSpec extends GatlingHttpFunSpec with StringOps {
  // TODO - Remove hardcoded URI
  def baseURL: String = "http://127.0.0.1:8881/sot-api/2"

  override def httpConf: HttpProtocolBuilder =
    super.httpConf
      .disableResponseChunksDiscarding // TODO - Make configurable especially for performance test as it can slows things down
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .extraInfoExtractor { extraInfo =>
        println("Response:")
        println(extraInfo.response.body.string.parseJson.prettyPrint)
        Nil
      }

  spec {
    http(getClass.getSimpleName.title)
      .get("/version")
    //.check(h1.exists)
  }
}