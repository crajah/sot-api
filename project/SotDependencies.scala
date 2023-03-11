import sbt._

object SotDependencies {
  val sotApiCommon = "parallelai" %% "sot-api-common" % "0.1.38"
  val sotCommonSecure = "parallelai" %% "sot_common_secure" % "0.1.29"
  val sotExecutorModel = "parallelai" %% "sot_executor_model" % "0.1.54"
  val sotCommonPersist = "parallelai" %% "sot_common_persist" % "0.2.19"
  val sotGcp = "parallelai" %% "sot_gcp" % "0.1.3"
  val sotContainers = "parallelai" %% "sot_containers" % "0.1.7"
}