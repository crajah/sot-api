package parallelai.sot.api.mechanics

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.Monad
import cats.implicits._
import com.google.api.services.dataflow.model.Job
import parallelai.sot.api.config.executor.google.dataflowRegion

trait DataFlowRepository[F[_]] {
  def findAllJobs(projectId: String): F[Seq[Job]]

  def findJobByRuleId(projectId: String, ruleId: String): F[Option[Job]]
}

class DataFlowProgram[F[_]: Monad](dataFlowRepo: DataFlowRepository[F]) {
  def findAllJobsByProjectId(projectId: String): F[Seq[Job]] =
    dataFlowRepo.findAllJobs(projectId).flatMap { jobs: Seq[Job] =>
      implicitly[Monad[F]].pure(jobs)
    }

  def findJobByRuleId(projectId: String, ruleId: String): F[Option[Job]] =
    findAllJobsByProjectId(projectId).map(_.find(job => job.getName == ruleId))
}

trait DataFlowProgramFutureInterpreter extends DataFlowRepository[Future] with GoogleCloudClient {
  override def findAllJobs(projectId: String): Future[Seq[Job]] =
    Future(dataflow.projects.locations.jobs.list(projectId, dataflowRegion).execute.getJobs.asScala)

  override def findJobByRuleId(projectId: String, ruleId: String): Future[Option[Job]] =
    findAllJobs(projectId).map(_.find(_.getName == ruleId))
}
