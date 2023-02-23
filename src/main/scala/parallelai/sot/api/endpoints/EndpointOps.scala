package parallelai.sot.api.endpoints

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.finch.Output
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.finagle.http.Status
import parallelai.sot.api.actions.{ Response, ResponseException }
import parallelai.sot.api.entities._

trait EndpointOps {
  type TFuture[A] = com.twitter.util.Future[A]

  // TODO Monix Task, then have toTFuture that converts Task -> Future -> Twitter Future

  implicit class FutureResponseOps[T](f: Future[Response]) {
    def toOutput: Future[Output[Response]] = f recover {
      case ResponseException(r) => r
      case t: Throwable => Response(Error(t.getMessage), Status.InternalServerError)
    } map { callResponse =>
      Output.payload(callResponse, callResponse.status)
    }

    def toTFuture: TFuture[Output[Response]] =
      toOutput.as[TFuture[Output[Response]]]
  }

  implicit class ResponseOps(r: Response) {
    import cats.implicits._

    def toTFuture: TFuture[Output[Response]] = r.pure[Future].toTFuture
  }

  implicit class PathContext(val sc: StringContext) {
    def p(args: Any*): String =
      sc.s(args: _*).replaceAll(" ", "").replaceAll("::", "/")
  }
}