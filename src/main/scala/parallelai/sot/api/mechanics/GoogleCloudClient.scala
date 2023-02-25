package parallelai.sot.api.mechanics

import java.util.Collections
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.dataflow.Dataflow
import parallelai.sot.api.config.api

object GoogleJobStatus {
  type JobStatus = String

  val JOB_STATE_STOPPED: JobStatus = "JOB_STATE_STOPPED"
  val JOB_STATE_RUNNING: JobStatus = "JOB_STATE_RUNNING"
  val JOB_STATE_DRAINED: JobStatus = "JOB_STATE_DRAINED"
  val JOB_STATE_CANCELLED: JobStatus = "JOB_STATE_CANCELLED" // TODO - Maybe "drained" should be default and then have "cancel" as query param to force stop
}

trait GoogleCloudClient {
  private val httpTransport = GoogleNetHttpTransport.newTrustedTransport

  private val jsonFactory = JacksonFactory.getDefaultInstance

  protected lazy val dataflow: Dataflow = new Dataflow.Builder(httpTransport, jsonFactory, credential).setApplicationName(api.name).build

  protected lazy val credential: GoogleCredential = {
    // Authentication is provided by gcloud tool when running locally and by built-in service accounts when running on GAE, GCE or GKE.
    val credential = GoogleCredential.getApplicationDefault

    // The createScopedRequired method returns true when running on GAE or a local developer machine.
    // In that case, the desired scopes must be passed in manually. When the code is
    // running in GCE, GKE or a Managed VM, the scopes are pulled from the GCE metadata server.
    // See https://developers.google.com/identity/protocols/application-default-credentials for more information.
    if (credential.createScopedRequired) {
      credential.createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"))
    } else {
      credential
    }
  }
}