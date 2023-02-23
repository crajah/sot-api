package parallelai.sot.api.mechanics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.data.OptionT
import cats.implicits._
import com.google.api.services.dataflow.model.Job
import parallelai.sot.api.config.executor.google.dataflowRegion
import parallelai.sot.api.mechanics.GoogleJobStatus._

trait DataflowService extends GoogleCloudClient {
  def updateJob(job: Job, projectId: String, newState: JobStatus): Job =
    dataflow.projects.locations.jobs.update(projectId, dataflowRegion, job.getId, job.setRequestedState(newState)).execute()
}

trait DataflowMechanic extends DataflowService {
  private val interpreter = new DataFlowProgramFutureInterpreter {}
  val googleRepo = new DataFlowProgram(interpreter)

  def updateStateIfRunning(projectId: String, ruleId: String, newState: JobStatus): OptionT[Future, Job] =
    for {
      job <- OptionT(googleRepo.findJobByRuleId(projectId, ruleId))
      updatedJob <- OptionT.fromOption[Future](update(job, projectId, newState))
    } yield updatedJob

  def update(job: Job, projectId: String, newState: JobStatus): Option[Job] =
    if (isRunning(job)) Some(updateJob(job, projectId, newState))
    else None

  private val isRunning: Job => Boolean =
    _.getCurrentState == JOB_STATE_RUNNING
}