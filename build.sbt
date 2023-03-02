import scala.language.postfixOps
import sbt.Resolver
import sbt.Keys.{libraryDependencies, publishTo}
import com.amazonaws.regions.{Region, Regions}
import Dependencies._
import SotDependencies._

lazy val scala_2_11 = "2.11.11"
lazy val scala_2_12 = "2.12.4"

lazy val sbt_0_13_15 = "0.13.15"
lazy val sbt_1_1_1 = "1.1.1"

lazy val assemblySettings = assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.concat
  case "project.properties" => MergeStrategy.concat
  case "BUILD" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".jar" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

lazy val IT = config("it") extend Test

lazy val root = (project in file(".")).enablePlugins(GatlingPlugin, DockerPlugin, DockerComposePlugin)
  .configs(IT, IntegrationTest, GatlingIt)
  .settings(Defaults.itSettings: _*)
  .settings(inConfig(IT)(Defaults.testSettings): _*)
  .settings(javaOptions in IT ++= Seq("-Dconfig.resource=application.it.conf", "-Dlogback.configurationFile=./src/it/resources/logback-test.xml"))
  .settings(Revolver.settings)
  .settings(
    name := "sot-api",
    inThisBuild(
      List(
        organization := "parallelai",
        scalaVersion := scala_2_12
      )
    ),
    scalacOptions ++= Seq(
      "–explaintypes",
      "–optimise",
      "–verbose",
      "-deprecation",           // Emit warning and location for usages of deprecated APIs.
      "-feature",               // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked",             // Enable additional warnings where generated code depends on assumptions.
      "-Xlint",                 // Enable recommended additional warnings.
      "-Ywarn-adapted-args",    // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",   // Warn when non-Unit expression results are unused.
      "-Ypartial-unification",
      "-language:postfixOps",
      "-language:higherKinds",
      "-language:existentials"/*,
      "-Xlog-implicits"*/
    ),
    crossScalaVersions := Seq(scala_2_11, scala_2_12),
    assemblySettings,
    dockerImageCreationTask := docker.value,
    s3region := Region.getRegion(Regions.EU_WEST_2),
    publishTo := {
      val prefix = if (isSnapshot.value) "snapshot" else "release"
      Some(s3resolver.value(s"Parallel AI $prefix S3 bucket", s3(s"$prefix.repo.parallelai.com")) withMavenPatterns)
    },
    resolvers ++= Seq[Resolver](
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      s3resolver.value("Parallel AI S3 Releases resolver", s3("release.repo.parallelai.com")) withMavenPatterns,
      s3resolver.value("Parallel AI S3 Snapshots resolver", s3("snapshot.repo.parallelai.com")) withMavenPatterns
    ),
    resolvers += sbtResolver.value,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      scalatest % "test, it",
      mockitoScala % "test, it",
      testContainers % "test, it",
      gatlingHighcharts % "it",
      gatlingTestFramework % "it"
    ),
    libraryDependencies ++= Seq(
      sotContainers % "it" classifier "it"
    ),
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-xml_2.11",
      "org.scala-lang.modules" % "scala-parser-combinators_2.11"
    ),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
      shapeless,
      typesafeConfig,
      pureConfig,
      grizzledLogging,
      logback,
      eclipseGit,
      commonsIo,
      gcloudDatastore,
      gcloudStorage,
      gcloudDataflow,
      shapelessDataTypeCore,
      shapelessDataTypeDatastore,
      jodaTime,
      twitterBijectionCore,
      twitterBijectionUtil,
      twitterServer,
      twitterFinagleHttp,
      twitterFinagleNetty,
      twitterFinagleStats,
      twitterUtil,
      twitterServerLogback,
      finchCore,
      finchGeneric,
      finchSprayJson,
      jsonLenses,
      sprayJsonShapeless,
      finchOAuth2,
      betterFiles,
      sttp,
      sttpOkhttpBackend,
      sttpMonixBackend,
      sttpCirce,
      monocleCore,
      monocleMacro,
      commonsLang
    ),
    libraryDependencies ++= Seq(
      sotExecutorModel,
      sotCommonPersist,
      sotCommon,
      sotCommonSecure,
      sotGcp
    )
  )

fork in run := true

fork in Test := true

fork in IT := true

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  /**
    * docker run --rm -it -p 8082:8082 -p 9092:9092 -v ~/.aws/credentials:/root/.aws/credentials:ro -v ~/.config/gcloud/application_default_credentials:/root/.config/gcloud/application_default_credentials -e GOOGLE_APPLICATION_CREDENTIALS=/root/.config/gcloud/application_default_credentials --name sot-api parallelai/sot-api
    */
  new Dockerfile {
    from("hseeberger/scala-sbt")
    env("SCALA_VERSION", scala_2_12)
    env("SBT_VERSION", sbt_1_1_1)
    expose(8082)
    expose(9092)
    volume("/executor")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-Xms1024m", "-Xmx2048m", "-jar", artifactTargetPath)
  }
}

buildOptions in docker := BuildOptions(cache = false)