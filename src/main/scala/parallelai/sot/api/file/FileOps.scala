package parallelai.sot.api.file

trait FileOps {
  val jarName: String => String = { name =>
    s"$name.jar"
  }
}