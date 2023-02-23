package parallelai.sot.api.disjunction

import cats.data.EitherT
import cats.implicits._
import cats.{ Applicative, Monad }

trait EitherOps {
  def left[F[_]: Applicative, L, R](l: L): EitherT[F, L, R] =
    EitherT.left[R](l.pure[F])

  def right[F[_]: Applicative, L, R](r: R): EitherT[F, L, R] =
    EitherT.right[L](r.pure[F])

  def flatten[F[_]: Monad, T](e: EitherT[F, T, T]): F[T] =
    implicitly[Monad[F]].map(e.value) {
      case Left(x) => x
      case Right(x) => x
    }
}

object EitherOps extends EitherOps