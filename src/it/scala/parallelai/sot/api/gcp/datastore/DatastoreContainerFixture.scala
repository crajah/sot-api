package parallelai.sot.api.gcp.datastore

import java.util.concurrent.TimeUnit
import org.scalatest.Suite
import com.google.cloud.datastore.{KeyQuery, Query, ReadOption}
import parallelai.common.persist.datastore.shapeless.Datastore
import parallelai.sot.containers.ContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

trait DatastoreContainerFixture extends parallelai.sot.containers.gcp.datastore.DatastoreContainerFixture {
  this: Suite with ContainersFixture with ProjectFixture =>

  /*def datastore[I <: IdBase : TypeTag : ClassTag] = {
    new Datastore[I](datastoreOptions = Option(DatastoreOptions.newBuilder().setHost(datastoreContainer.ip).build()))
  }*/

  //lazy val datastore: Datastore[I] //= new DatastoreContainer(8081)
  //def datastore[I <: IdBase : TypeTag : ClassTag] = new Datastore[I](datastoreOptions = Option(DatastoreOptions.newBuilder().setHost(datastoreContainer.ip).build()))

  /*override def teardown(): Unit = {
    val queryAllKeys: KeyQuery = Query.newKeyQueryBuilder().setKind(kind.value).build()
    val result = datastore.getUnderlying.datastore.run(queryAllKeys, ReadOption.eventualConsistency)

    while (result.hasNext) {
      datastore.getUnderlying.datastore.delete(result.next)
    }
  }*/

  def teardown(datastores: Seq[Datastore[_]]): Unit = datastores foreach { datastore =>
    val queryAllKeys: KeyQuery = Query.newKeyQueryBuilder().setKind(datastore.getUnderlying.kind).build()
    val result = datastore.getUnderlying.datastore.run(queryAllKeys, ReadOption.eventualConsistency)

    while (result.hasNext) {
      datastore.getUnderlying.datastore.delete(result.next)
    }

    TimeUnit.SECONDS.sleep(1) // TODO Gimmick
  }
}