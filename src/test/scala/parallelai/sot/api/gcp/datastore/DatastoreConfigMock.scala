package parallelai.sot.api.gcp.datastore

import org.scalatest.mockito.MockitoSugar
import com.google.cloud.datastore.DatastoreOptions

trait DatastoreConfigMock extends DatastoreConfig with MockitoSugar {
  override protected lazy val datastoreOptions: DatastoreOptions = mock[DatastoreOptions]
}