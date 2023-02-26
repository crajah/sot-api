package parallelai.sot.api

import scala.reflect.ClassTag
import better.files.File
import grizzled.slf4j.Logging
import monocle.macros.syntax.lens._
import monocle.std.option._
import pureconfig.ConvertHelpers._
import pureconfig._

package object config extends Logging {
  implicit val fileReader: ConfigReader[File] = ConfigReader.fromString[File](catchReadError(File(_)))

  lazy val baseDirectory: File = load[File]("baseDirectory")

  lazy val secret: String = load[String]("secret")

  lazy val api: Api = {
    val e = load[Api]("api")
    info(s"API configuration: $e")
    e
  }

  lazy val licence: Licence = {
    val e = load[Licence]("licence")
    info(s"Licence configuration: ${e.lens(_.apiKey) composePrism some set "<masked api key>"}")
    e
  }

  lazy val executor: Executor = {
    val e = load[Executor]("executor")
    info(s"Executor configuration: $e")
    e
  }

  def load[C: ClassTag](namespace: String)(implicit reader: Derivation[ConfigReader[C]]): C =
    loadConfigOrThrow[C](namespace)
}