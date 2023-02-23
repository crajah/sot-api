package parallelai.sot.api.gcp.datastore

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import cats.implicits._
import shapeless.HList
import shapeless.LabelledGeneric.Aux
import shapeless.datatype.datastore.{ FromEntity, ToEntity }
import com.google.cloud.datastore.DatastoreOptions
import parallelai.common.persist.Identity
import parallelai.common.persist.datastore.shapeless._
import parallelai.sot.api.concurrent.DatastoreExcecutionContext
import parallelai.sot.api.config.executor

trait DatastoreConfig {
  protected lazy val datastoreKindPrefix: String = {
    val kind = executor.dao.prefix

    // TODO - This should be in underlying perist module (or should it)
    if (kind.endsWith("-")) kind else s"$kind-"
  }

  protected lazy val datastoreOptions: DatastoreOptions = DatastoreOptions.getDefaultInstance

  def datastore[T: Identity: TypeTag: ClassTag]: ApiDatastore[T] =
    new ApiDatastore[T]

  class ApiDatastore[T: Identity: TypeTag: ClassTag] {
    val datastore = new Datastore[T](datastoreKindPrefix, Option(datastoreOptions))

    implicit val ec: ExecutionContext = DatastoreExcecutionContext()

    def newKey[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[datastore.KEY] = ().pure[Future].flatMap { _ =>
      datastore newKey item
    }(ec)

    def newKey[L <: HList](id: String)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[datastore.KEY] = ().pure[Future].flatMap { _ =>
      datastore newKey id
    }(ec)

    def insert[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[T] = ().pure[Future].flatMap { _ =>
      datastore insert item
    }(ec)

    def put[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[T] = ().pure[Future].flatMap { _ =>
      datastore put item
    }(ec)

    def findOne[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[T]] = ().pure[Future].flatMap { _ =>
      datastore findOne item
    }(ec)

    def findOneById[L <: HList](id: String)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Option[T]] = ().pure[Future].flatMap { _ =>
      datastore findOneById id
    }(ec)

    def findAll[L <: HList](implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[T]] = ().pure[Future].flatMap { _ =>
      datastore.findAll
    }(ec)

    def findAllBy[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[T]] = ().pure[Future].flatMap { _ =>
      datastore findAllBy item
    }(ec)

    def findAllById[L <: HList](id: String)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[List[T]] = ().pure[Future].flatMap { _ =>
      datastore findAllById id
    }(ec)

    def update[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[T] = ().pure[Future].flatMap { _ =>
      datastore update item
    }(ec)

    def updateAndForget[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Unit] = ().pure[Future].flatMap { _ =>
      datastore updateAndForget item
    }(ec)

    def delete[L <: HList](item: T)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Unit] = ().pure[Future].flatMap { _ =>
      datastore delete item
    }(ec)

    def deleteById[L <: HList](id: String)(implicit gen: Aux[T, L], toL: ToEntity[L], fromL: FromEntity[L]): Future[Unit] = ().pure[Future].flatMap { _ =>
      datastore deleteById id
    }(ec)
  }
}