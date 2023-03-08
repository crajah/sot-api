package parallelai.sot.api.model

import parallelai.common.persist.Identity

case class Dag(id: String, name: String, edges: Edge*)

object Dag {
  implicit val dagIdentity: Identity[Dag] = Identity[Dag](_.id)
}

case class Edge(from: String, to: String) // TODO tag these to distinguish them