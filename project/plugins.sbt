resolvers ++= Seq[Resolver](
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "Era7 maven releases" at "https://s3-eu-west-1.amazonaws.com/releases.era7.com",
  Resolver.jcenterRepo,
  Resolver.sbtPluginRepo("releases"),
  Classpaths.sbtPluginReleases
)

// addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0") TODO - Need extra coursier to work with S3

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.17.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

addSbtPlugin("com.tapad" % "sbt-docker-compose" % "1.0.34")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")

addSbtPlugin("io.gatling" % "gatling-sbt" % "2.2.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")

addSbtPlugin("com.scalapenos" % "sbt-prompt" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")