package parallelai.sot.api

import scala.concurrent.Future
import io.finch._
import io.finch.sprayjson._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.service.HttpResponseClassifier
import com.twitter.finagle.http.{ Request, Response }
import com.twitter.finagle.param.Label
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{ Http, Service }
import com.twitter.util.Await
import parallelai.sot.api.concurrent.ExecutionContexts._
import parallelai.sot.api.config.api
import parallelai.sot.api.endpoints._
import parallelai.sot.api.gcp.datastore.DatastoreConfig

object Bootstrap extends TwitterServer with DatastoreConfig
  with HealthEndpoints with RuleEndpoints with VersionEndpoints with EnvEndpoints with StepEndpoints with TapEndpoints
  with DagEndpoints with SourceEndpoints with SchemaEndpoints with LookupEndpoints with FolderEndpoints with LcmEndpoints {

  // val port: Flag[Int] = flag("port", 8082 /*SERVER_PORT*/ , "TCP port for HTTP server") // TODO Is this required?

  implicit val backend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()

  val service: Service[Request, Response] = (
    healthEndpoints :+: ruleEndpoints :+: versionEndpoints :+: envEndpoints :+: stepEndpoints :+: lookupEndpoints :+: tapEndpoints :+:
    dagEndpoints :+: sourceEndpoints :+: schemaEndpoints :+: folderEndpoints :+: lcmEndpoints).toServiceAs[Application.Json]

  implicit class CorsService(service: Service[Request, Response]) {
    def withCORSSupport: Service[Request, Response] = {
      val policy: Cors.Policy = Cors.Policy(
        allowsOrigin = _ => Some("*"),
        allowsMethods = _ => Some(Seq("GET", "POST", "PUT", "DELETE")),
        allowsHeaders = _ => Some(Seq("Accept")))

      new Cors.HttpFilter(policy) andThen service
    }
  }

  implicit val stats: StatsReceiver = statsReceiver

  def main(): Unit = {
    val server = Http.server
      .configured(Label(s"https/${api.name}"))
      .withStatsReceiver(statsReceiver)
      .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)
      .withHttpStats
      .serve(s"${api.uri.getHost}:${api.uri.getPort}", service.withCORSSupport)

    logger.info(s"HTTP server started on: ${server.boundAddress}")

    closeOnExit(server)

    Await.all(server, adminHttpServer)
  }
}

/*
TODO - Some more CORS stuff possible?

def withCorsHeaders[T] = { endpoint: Endpoint[T] =>
    endpoint
      .withHeader(("Access-Control-Allow-Origin", "*"))
      .withHeader(
        ("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH")
      )
      .withHeader(("Access-Control-Max-Age", "3600"))
      .withHeader(("Access-Control-Allow-Headers", """Content-Type,
          |Cache-Control,
          |Content-Language,
          |Expires,
          |Last-Modified,
          |Pragma,
          |X-Requested-With,
          |Origin,
          |Accept
        """.stripMargin.filter(_ >= ' ')))
  }
 */ 