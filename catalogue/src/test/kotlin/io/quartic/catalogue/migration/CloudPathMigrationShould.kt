package io.quartic.catalogue.migration

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetLocator.GeoJsonDatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import org.junit.Test

class CloudPathMigrationShould {
    private val backend = mock<StorageBackend>()
    private val migration = CloudPathMigration()

    @Test
    fun ignore_non_cloud_datasets() {
        val config = mock<DatasetConfig> {
            on { locator } doReturn GeoJsonDatasetLocator("blah")
        }
        whenever(backend.getAll()).thenReturn(mapOf(mock<DatasetCoordinates>() to config))

        migration.migrate(backend)

        verify(backend, only()).getAll()
    }

    @Test
    fun ignore_already_migrated_datasets() {
        val config = mock<DatasetConfig> {
            on { locator } doReturn CloudDatasetLocator("/foo/bar/baz", false, "whatever")
        }
        whenever(backend.getAll()).thenReturn(mapOf(mock<DatasetCoordinates>() to config))

        migration.migrate(backend)

        verify(backend, only()).getAll()
    }

    @Test
    fun migrate_2d_datasets() {
        val coords = mock<DatasetCoordinates>()
        val metadata = mock<DatasetMetadata>()
        val config = DatasetConfig(
            metadata = metadata,
            locator = CloudDatasetLocator("/foo/bar", false, "whatever")
        )
        whenever(backend.getAll()).thenReturn(mapOf(coords to config))

        migration.migrate(backend)

        verify(backend)[coords] = DatasetConfig(
            metadata,
            CloudDatasetLocator("/foo/foo/bar", false, "whatever")
        )
    }
}
