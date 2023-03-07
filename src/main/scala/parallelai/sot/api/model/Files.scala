package parallelai.sot.api.model

import better.files.File

object Files {
  class ApplicationConfigFile(val file: File) extends AnyVal

  class RuleFile(val file: File) extends AnyVal
}