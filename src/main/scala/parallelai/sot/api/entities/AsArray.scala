package parallelai.sot.api.entities

trait AsArray[T] {
  def apply(t: T): Array[Byte]
}

object AsArray {
  implicit val stringAsArray: AsArray[String] = _.getBytes

  def apply[T: AsArray]: AsArray[T] = implicitly[AsArray[T]]
}