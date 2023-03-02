import sbt._

object Dependencies {
  val pureConfigVersion = "0.9.0"
  val gcloudVersion = "1.6.0"
  val shapelessDataTypeDataVersion = "0.1.7"
  val twitterBijectionVersion = "0.9.6"
  val twitterVersion = "18.2.0"
  val finchVersion = "0.17.0"
  val sttpVersion = "1.1.9"
  val gatlingVersion = "2.2.5"
  val monocleVersion = "1.5.0"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.4"
  val mockitoScala = "org.markushauck" %% "mockitoscala" % "0.3.0"
  val testContainers = "com.dimafeng" %% "testcontainers-scala" % "0.11.0"

  val typesafeConfig = "com.typesafe" % "config" % "1.3.3"
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion

  val grizzledLogging = "org.clapper" %% "grizzled-slf4j" % "1.3.2"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val eclipseGit = "org.eclipse.jgit" % "org.eclipse.jgit" % "4.10.0.201712302008-r"

  val commonsIo = "commons-io" % "commons-io" % "2.5"

  val gcloudDatastore = "com.google.cloud" % "google-cloud-datastore" % gcloudVersion
  val gcloudStorage = "com.google.cloud" % "google-cloud-storage" % gcloudVersion //"1.7.0"
  val gcloudDataflow = "com.google.apis" % "google-api-services-dataflow" % "v1b3-rev214-1.22.0"

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
  val shapelessDataTypeCore = "me.lyh" %% "shapeless-datatype-core" % shapelessDataTypeDataVersion
  val shapelessDataTypeDatastore = "me.lyh" %% "shapeless-datatype-datastore_1.3" % shapelessDataTypeDataVersion

  val jodaTime = "joda-time" % "joda-time" % "2.9.9"

  val twitterBijectionCore = "com.twitter" %% "bijection-core" % twitterBijectionVersion
  val twitterBijectionUtil = "com.twitter" %% "bijection-util" % twitterBijectionVersion

  val twitterServer = "com.twitter" %% "twitter-server" % twitterVersion
  val twitterFinagleHttp = "com.twitter" %% "finagle-http" % twitterVersion
  val twitterFinagleNetty = "com.twitter" %% "finagle-netty3" % twitterVersion
  val twitterFinagleStats = "com.twitter" %% "finagle-stats" % twitterVersion
  val twitterUtil = "com.twitter" %% "util-collection" % twitterVersion
  val twitterServerLogback = "com.twitter" %% "twitter-server-logback-classic" % twitterVersion

  val finchCore = "com.github.finagle" %% "finch-core" % finchVersion
  val finchGeneric = "com.github.finagle" %% "finch-generic" % finchVersion
  val finchSprayJson = "com.github.finagle" %% "finch-sprayjson" % finchVersion
  val finchOAuth2 = "com.github.finagle" %% "finch-oauth2" % "0.16.1"

  val sprayJson = "io.spray" %%  "spray-json" % "1.3.4"
  val sprayJsonShapeless = "com.github.fommil" %% "spray-json-shapeless" % "1.3.0"
  val jsonLenses = "net.virtual-void" %%  "json-lenses" % "0.6.2"

  val betterFiles = "com.github.pathikrit" %% "better-files" % "3.4.0"

  val sttp = "com.softwaremill.sttp" %% "core" % sttpVersion
  val sttpOkhttpBackend = "com.softwaremill.sttp" %% "okhttp-backend" % sttpVersion
  val sttpMonixBackend = "com.softwaremill.sttp" %% "async-http-client-backend-monix" % sttpVersion
  val sttpCirce = "com.softwaremill.sttp" %% "circe" % sttpVersion

  val gatlingHighcharts = "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion
  val gatlingTestFramework = "io.gatling" % "gatling-test-framework" % gatlingVersion

  val monocleCore = "com.github.julien-truffaut" %% "monocle-core" % monocleVersion
  val monocleMacro = "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion

  val commonsLang = "org.apache.commons" % "commons-lang3" % "3.7"
}