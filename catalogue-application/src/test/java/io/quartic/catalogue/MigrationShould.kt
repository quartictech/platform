package io.quartic.catalogue

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.Migration.Companion.BACKUP_NAMESPACE
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetLocator.*
import io.quartic.catalogue.api.model.MimeType
import org.junit.Test
import java.io.IOException

class MigrationShould {
    private val config = DatasetConfig(
            metadata = mock(),
            locator = CloudGeoJsonDatasetLocator("foo", true),
            extensions = mapOf("foo" to "bar")
    )
    private val backend = mock<StorageBackend> {
        on { all } doReturn mapOf(DatasetCoordinates("here", "there") to config)
    }

    @Test
    fun overwrite_locator() {
        Migration().migrate(backend)

        verify(backend).put(
                DatasetCoordinates("here", "there"),
                config.copy(locator = CloudDatasetLocator("foo", true, MimeType.GEOJSON))
        )
    }

    @Test
    fun create_and_delete_backup_in_correct_sequence() {
        Migration().migrate(backend)

        val backupCoords = DatasetCoordinates(BACKUP_NAMESPACE, DatasetId("there"))
        inOrder(backend) {
            verify(backend).put(backupCoords, config)
            verify(backend).put(any(), any())
            verify(backend).remove(backupCoords)
        }
    }

    @Test
    fun not_delete_backup_on_failure() {
        whenever(backend.put(eq(DatasetCoordinates("here", "there")), any()))
                .thenThrow(IOException("emo"))

        Migration().migrate(backend)

        val backupCoords = DatasetCoordinates(BACKUP_NAMESPACE, DatasetId("there"))
        verify(backend, never()).remove(backupCoords)
    }

    @Test
    fun skip_non_cloud_geojson_locators() {
        val config = mock<DatasetConfig>()
        whenever(config.locator).thenReturn(mock<PostgresDatasetLocator>())
        whenever(backend.all).thenReturn(mapOf(mock<DatasetCoordinates>() to config))

        Migration().migrate(backend)

        verify(backend).all
        verifyNoMoreInteractions(backend)
    }
}