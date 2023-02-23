package parallelai.sot.api.identity

object IdGenerator extends IdGenerator

trait IdGenerator {
  def uniqueId(id: String = ""): String = {
    val uniqueId = id.trim.replaceAll("\\s+", "").stripMargin.stripLineEnd.toLowerCase

    if (uniqueId.isEmpty) {
      uniqueSuffix
    } else {
      s"$uniqueId-$uniqueSuffix"
    }
  }

  protected def uniqueSuffix: String = System.currentTimeMillis.toString
}