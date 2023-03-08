package parallelai.sot.api.actions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.data.Validated._
import cats.data._
import cats.implicits._
import shapeless.LabelledGeneric.Aux
import shapeless._
import shapeless.datatype.datastore._
import parallelai.common.persist.Identity
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.endpoints.Response.Error
import parallelai.sot.api.model.{TapWrapper, _}
import parallelai.sot.executor.model.SOTMacroConfig._

trait DagActions extends EntityFormats with DatastoreMappableType {
  this: DatastoreConfig =>

  import parallelai.sot.api.actions.DagActions._

  // TODO - Sort out as all over the place
  implicit private val schemaIdentity: Identity[Schema] = Identity[Schema](_.id)
  implicit private val schemaWrapperIdentity: Identity[SchemaWrapper] = Identity[SchemaWrapper](_.id)
  implicit private val tapIdentity: Identity[TapDefinition] = Identity[TapDefinition](_.id)
  implicit private val tapWrapperIdentity: Identity[TapWrapper] = Identity[TapWrapper](_.id)
  implicit private val opTypeIdentity: Identity[OpType] = Identity[OpType](_.id)
  implicit private val opTypeWrapperIdentity: Identity[OpTypeWrapper] = Identity[OpTypeWrapper](_.id)

  lazy val dagDAO = datastore[Dag]
  lazy val schemaDAO = datastore[SchemaWrapper]
  lazy val tapDAO = datastore[TapWrapper]
  lazy val opTypeDAO = datastore[OpTypeWrapper]

  def buildDag(id: String): Future[Either[Error, RuleComposites]] = (for {
    dag <- EitherT.fromOptionF(dagDAO findOneById id, Error(s"Invalid DAG ID $id"))
    ruleComponents <- EitherT(ruleComposites(dag))
  } yield {
    println(s"===> rule composites = $ruleComponents") // TODO - Remove
    ruleComponents
  }).value

  def ruleComposites(dag: Dag): Future[Either[Error, RuleComposites]] = {
    def find[T, L <: HList](d: ApiDatastore[T])(implicit ruleCompositeId: String, gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]) =
      d findOneById ruleCompositeId

    val ruleCompsiteIds = dag.edges.flatMap(e => Seq(e.from, e.to)).distinct

    val ruleComposites: Future[Seq[RuleComposites]] = Future.sequence(ruleCompsiteIds.map { implicit ruleCompositeId =>
      (find(schemaDAO), find(tapDAO), find(opTypeDAO)).mapN { (a, p, o) =>
        RuleComposites(Seq(a.map(_.schema)).flatten, Seq(p.map(_.tap)).flatten, Seq(o.map(_.opType)).flatten, dag.edges)
      }
    })

    ruleComposites.map(_.foldLeft(RuleComposites(dag = dag.edges)) { (acc, rs) =>
      RuleComposites(acc.schemas ++ rs.schemas, acc.taps ++ rs.taps, acc.opTypes ++ rs.opTypes, dag.edges)
    }).map(validate(dag))
  }
}

object DagActions {
  def validate(dag: Dag)(rs: RuleComposites): Either[Error, RuleComposites] = {
    val validations = (validate("Schemas", rs.schemas), validate("Taps", rs.taps), validate("Steps", rs.opTypes), validNel(rs.dag)).mapN(RuleComposites.apply)

    validations.toEither.leftMap(nel => Error(s"DAG ${dag.name} (ID ${dag.id}) has no associated ${nel.toList.mkString("; ")}"))
  }

  def validate[T](ruleCompositeType: String, oneTypeOfRuleComposites: Seq[T]): ValidatedNel[String, Seq[T]] =
    oneTypeOfRuleComposites match {
      case Nil => invalidNel(ruleCompositeType)
      case _ => valid(oneTypeOfRuleComposites)
    }
}