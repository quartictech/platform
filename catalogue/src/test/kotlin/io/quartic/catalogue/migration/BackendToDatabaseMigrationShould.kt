package io.quartic.catalogue.migration

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.catalogue.database.Database
import org.junit.Test

class BackendToDatabaseMigrationShould {
    private val namespaceA = DatasetNamespace("foo")
    private val idA = DatasetId("alice")
    private val configA = mock<DatasetConfig>()
    private val namespaceB = DatasetNamespace("bar")
    private val idB = DatasetId("bob")
    private val configB = mock<DatasetConfig>()

    private val backend = mock<StorageBackend> {
        on { getAll() } doReturn mapOf(
            DatasetCoordinates(namespaceA, idA) to configA,
            DatasetCoordinates(namespaceB, idB) to configB
        )
    }
    private val database = mock<Database>()
    private val migration = BackendToDatabaseMigration()

    @Test
    fun migrate_datasets() {
        migration.migrate(backend, database)

        verify(database).insertDataset("foo", "alice", configA)
        verify(database).insertDataset("bar", "bob", configB)
    }

    @Test
    fun skip_datasets_already_migrated() {
        whenever(database.getDataset("foo", "alice")).thenReturn(mock())

        migration.migrate(backend, database)

        verify(database).insertDataset("bar", "bob", configB)
        verify(database, times(1)).insertDataset(any(), any(), any())
    }
}
