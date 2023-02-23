package parallelai.sot.api.text

object StringOps extends StringOps

trait StringOps {
  implicit class StringOps(s: String) {
    lazy val title: String = s split "(?<!^)(?=[A-Z])" map (_.capitalize) mkString " "
  }
}