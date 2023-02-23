package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json._

case class Job(clientRequestId: Option[String], createTime: Option[String], currentState: Option[String],
  currentStateTime: Option[String], id: Option[String], location: Option[String], name: Option[String],
  projectId: Option[String], replaceJobId: Option[String], replacedByJobId: Option[String],
  requestedState: Option[String], jobType: Option[String])

object Job {
  implicit val rootJsonFormat: RootJsonFormat[Job] = jsonFormat12(Job.apply)

  implicit class GoogleToSotJob(gJob: com.google.api.services.dataflow.model.Job) {
    def toSotJob: Job = new Job(
      clientRequestId = Option(gJob.getClientRequestId),
      createTime = Option(gJob.getCreateTime),
      currentState = Option(gJob.getCurrentState),
      currentStateTime = Option(gJob.getCurrentStateTime),
      id = Option(gJob.getId),
      location = Option(gJob.getLocation),
      name = Option(gJob.getName),
      projectId = Option(gJob.getProjectId),
      replaceJobId = Option(gJob.getReplaceJobId),
      replacedByJobId = Option(gJob.getReplacedByJobId),
      requestedState = Option(gJob.getRequestedState),
      jobType = Option(gJob.getType))
  }
}