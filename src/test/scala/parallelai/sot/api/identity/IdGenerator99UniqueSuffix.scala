package parallelai.sot.api.identity

trait IdGenerator99UniqueSuffix extends IdGenerator {
  override protected def uniqueSuffix: String = "99"
}