package parallelai.sot.api.actions

import java.net.URI
import better.files.File
import org.scalatest.{MustMatchers, WordSpec}
import com.github.nscala_time.time.Imports.DateTime
import parallelai.sot.api.config._
import parallelai.sot.api.gcp.datastore.DatastoreConfig
import parallelai.sot.api.http.Errors
import parallelai.sot.api.model.{RegisteredVersion, Token}

class RuleActionsSpec extends WordSpec with MustMatchers {

  "Invoke google cloud for crypt file on buildRule" in new RuleActions with DatastoreConfig {
    val tag = "v0.1.14"
    val organisationCode = "organisationCode"
    val token = Token("licenceId", organisationCode, "me@gmail.com")
    val expectedUri = new URI("www.victorias-secret.com")
    val registeredVersion = RegisteredVersion(expectedUri, tag, token, DateTime.now)
    var mockUri: URI = _

    override def findCryptFile(uri: URI): Either[Errors, File] = {
      mockUri = uri
      Right(baseDirectory / "")
    }

    buildRule(registeredVersion)

    mockUri mustEqual expectedUri
  }
}
