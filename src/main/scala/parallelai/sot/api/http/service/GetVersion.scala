package parallelai.sot.api.http.service

import better.files.File
import cats.Monad
import parallelai.sot.api.model.RegisteredVersion

abstract class GetVersion[F[_]: Monad] {
  def apply(registeredVersion: RegisteredVersion): F[String Either File]
}