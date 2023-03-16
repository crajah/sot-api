package parallelai.sot.api.http.service

import java.net.URI
import better.files.File
import parallelai.sot.api.config._
import parallelai.sot.api.http.Errors

trait GoogleCloudService {

  def findCryptFile(uri: URI): Either[Errors, File] =
    Right(baseDirectory / "myfile.txt")
}
