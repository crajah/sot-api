package parallelai.sot.api.gcp.datastore

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import grizzled.slf4j.Logging
import org.scalatest.{BeforeAndAfterEach, Suite}
import com.google.cloud.datastore.DatastoreOptions
import parallelai.common.persist.Identity
import parallelai.common.persist.datastore.shapeless.Datastore
import parallelai.sot.api.config.baseDirectory
import parallelai.sot.containers.ContainersFixture
import parallelai.sot.containers.gcp.ProjectFixture

trait DatastoreFixture extends BeforeAndAfterEach with Logging {
  fixture: Suite with ContainersFixture with ProjectFixture with DatastoreContainerFixture =>

  private val datastores = ListBuffer[Datastore[_]]()

  override def setup(): Unit = if (baseDirectory.exists) {
    val directory = baseDirectory.delete()
    info(s"Deleted directory $directory")
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    teardown(datastores)
  }

  trait DatastoreITConfig extends DatastoreConfig {
    override protected lazy val datastoreKindPrefix: String = "it-"

    override protected lazy val datastoreOptions: DatastoreOptions = fixture.datastoreOptions

    override def datastore[T: Identity: TypeTag: ClassTag]: ApiDatastore[T] = {
      val ds = super.datastore
      datastores append ds.datastore
      ds
    }
  }
}