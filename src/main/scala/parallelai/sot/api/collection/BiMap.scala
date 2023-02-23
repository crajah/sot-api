package parallelai.sot.api.collection

class BiMap[X, Y](val m1: Map[X, Y], val m2: Map[Y, X]) {
  def apply(x: X): Y = m1(x)

  def apply(y: Y)(implicit d: BiMap.MethodDistinctor): X = m2(y)
}

object BiMap {
  protected[BiMap] trait MethodDistinctor

  implicit final object MethodDistinctor extends MethodDistinctor

  def apply[X, Y](tuples: (X, Y)*) = new BiMap(tuples.toMap, tuples.map(_.swap).toMap)

  def apply[X, Y](m: Map[X, Y]) = new BiMap(m, m.toSeq.map(_.swap).toMap)
}