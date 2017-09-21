package io.quartic.catalogue

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.model.*
import io.quartic.catalogue.database.Database
import io.quartic.catalogue.database.Database.CoordinatesAndConfig
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.sequenceGenerator
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Collections.emptyMap
import javax.websocket.Session
import javax.ws.rs.BadRequestException

class CatalogueResourceShould {
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val database = mock<Database>()
    private val resource = CatalogueResource(
            database,
            sequenceGenerator { uid: String -> DatasetId(uid) },
            clock
    )
    private val namespace = DatasetNamespace("foo")

    @Test
    fun get_specific_dataset() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(database.getDataset(namespace.namespace, id.uid)).thenReturn(config)

        assertThat(resource.getDataset(namespace, id), equalTo(config))
    }

    @Test
    fun get_all_datasets() {
        val datasets = loadsOfDatasets()

        whenever(database.getDatasets()).thenReturn(datasets)

        assertThat(resource.getDatasets(), equalTo(toMapOfMaps(datasets)))
    }

    @Test
    fun reject_registration_with_registered_timestamp_set() {
        assertThrows<BadRequestException> {
            resource.registerDataset(namespace, config(clock.instant()))
        }
    }

    @Test
    fun set_registered_timestamp_to_current_time() {
        val config = config()
        val configWithTimestamp = config(clock.instant())

        val coords = resource.registerDataset(namespace, config)

        verify(database).insertDataset(coords.namespace.namespace, coords.id.uid, configWithTimestamp)
    }

    @Test
    fun register_dataset_with_specified_id() {
        val config = config()
        val configWithTimestamp = config(clock.instant())

        val id = DatasetId("123")
        resource.registerOrUpdateDataset(namespace, id, config)

        verify(database).insertDataset(namespace.namespace, id.uid, configWithTimestamp)
    }

    @Test
    fun send_current_catalogue_state_on_websocket_open() {
        val session = mock<Session>(defaultAnswer = RETURNS_DEEP_STUBS)
        val datasets = loadsOfDatasets()

        whenever(database.getDatasets()).thenReturn(datasets)

        resource.onOpen(session, mock())

        verify(session.asyncRemote).sendText(serialize(toMapOfMaps(datasets)))
    }

    @Test
    fun send_catalogue_state_to_websocket_clients_after_change() {
        val session = mock<Session>(defaultAnswer = RETURNS_DEEP_STUBS)
        val datasets = loadsOfDatasets()

        whenever(database.getDatasets())
                .thenReturn(emptyList())
                .thenReturn(datasets)

        resource.onOpen(session, mock())
        resource.registerDataset(namespace, config())

        verify(session.asyncRemote).sendText(serialize(toMapOfMaps(datasets)))
        inOrder(database) {
            verify(database).getDatasets()   // This happens on session open
            verify(database).insertDataset(any(), any(), any())
            verify(database).getDatasets()   // We care that this happened *after* the put
        }
    }

    @Test
    fun not_send_state_to_closed_websockets_after_change() {
        val session = mock<Session>(defaultAnswer = RETURNS_DEEP_STUBS)

        resource.onOpen(session, mock())
        resource.onClose(session, mock())
        resource.registerDataset(namespace, config())

        verify(session.asyncRemote).sendText(serialize(emptyMap<Any, Any>()))   // Initial catalogue state
        verifyNoMoreInteractions(session.asyncRemote)
    }

    private fun toMapOfMaps(list: List<CoordinatesAndConfig>) = list
        .groupBy { DatasetNamespace(it.namespace) }
        .mapValues { it.value.associateBy({ DatasetId(it.id) }, { it.config }) }

    private fun serialize(obj: Any) = OBJECT_MAPPER.writeValueAsString(obj)

    private fun config(instant: Instant? = null) = DatasetConfig(
        DatasetMetadata("foo", "bar", "baz", instant),
        DatasetLocator.GeoJsonDatasetLocator("blah"),
        emptyMap<String, Any>()
    )

    private fun loadsOfDatasets(): List<CoordinatesAndConfig> = listOf(
        CoordinatesAndConfig(namespace.namespace, "alice", mock()),
        CoordinatesAndConfig(namespace.namespace, "bob", mock()),
        CoordinatesAndConfig("bar", "charlie", mock())  // Prove that multiple namespaces work ok
    )
}
