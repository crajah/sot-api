import sbt._

object SotDependencies {
  val sotExecutorModel = "parallelai" %% "sot_executor_model" % "0.1.54"
  val sotCommonPersist = "parallelai" %% "sot_common_persist" % "0.2.19"
  val sotCommon = "parallelai" %% "sot_common" % "0.1.7"
  val sotCommonSecure = "parallelai" %% "sot_common_secure" % "0.1.12"
  val sotGcp = "parallelai" %% "sot_gcp" % "0.1.3"
  val sotContainers = "parallelai" %% "sot_containers" % "0.1.7"
}