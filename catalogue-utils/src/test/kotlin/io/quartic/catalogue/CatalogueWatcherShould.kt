package io.quartic.catalogue

import com.fasterxml.jackson.databind.type.MapType
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.rx.all
import io.quartic.common.websocket.WebsocketListener
import org.hamcrest.Matchers.contains
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import rx.Observable.just

class CatalogueWatcherShould {
    private val listener = mock<WebsocketListener<Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>>>()
    private val listenerFactory = mock<WebsocketListener.Factory>()
    private val nsX = mock<DatasetNamespace>()
    private val nsY = mock<DatasetNamespace>()

    private val watcher = CatalogueWatcher(listenerFactory, setOf(nsX, nsY))

    @Before
    fun before() {
        whenever(listenerFactory.create<Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>>(any<MapType>()))
                .thenReturn(listener)
    }

    @Test
    fun emit_creation_event_when_dataset_appears() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(mapOf(nsX to mapOf(id to config))))

        assertThat(all(watcher.events), contains(CatalogueEvent(CREATE, coords(nsX, id), config)))
    }

    @Test
    fun emit_deletion_event_when_dataset_appears() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(
                mapOf(nsX to mapOf(id to config)),
                emptyMap()   // Gone!
        ))

        assertThat(all(watcher.events), contains(
                CatalogueEvent(CREATE, coords(nsX, id), config),
                CatalogueEvent(DELETE, coords(nsX, id), config)
        ))
    }

    @Test
    fun not_emit_creation_event_when_dataset_persists() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(
                mapOf(nsX to mapOf(id to config)),
                mapOf(nsX to mapOf(id to config))  // Again
        ))

        assertThat(all(watcher.events), contains(CatalogueEvent(CREATE, coords(nsX, id), config)))
    }

    @Test
    fun emit_events_for_multiple_independent_datasets() {
        val idA = mock<DatasetId>()
        val idB = mock<DatasetId>()
        val configA = mock<DatasetConfig>()
        val configB = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(
                mapOf(nsX to mapOf(idA to configA)),
                mapOf(nsX to mapOf(idA to configA, idB to configB)),
                mapOf(nsX to mapOf(idB to configB))
        ))

        assertThat(all(watcher.events), contains(
                CatalogueEvent(CREATE, coords(nsX, idA), configA),
                CatalogueEvent(CREATE, coords(nsX, idB), configB),
                CatalogueEvent(DELETE, coords(nsX, idA), configA)
        ))
    }

    @Test
    fun filter_out_unspecified_namespaces() {
        val idA = mock<DatasetId>()
        val idB = mock<DatasetId>()
        val configA = mock<DatasetConfig>()
        val configB = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(mapOf(
                nsX to mapOf(idA to configA),
                nsY to mapOf(idB to configB),
                mock<DatasetNamespace>() to mapOf(mock<DatasetId>() to mock<DatasetConfig>())   // Should get filtered out
        )))

        assertThat(all(watcher.events), contains(
                CatalogueEvent(CREATE, coords(nsX, idA), configA),
                CatalogueEvent(CREATE, coords(nsY, idB), configB)
        ))
    }

    @Test
    fun not_explode_if_specified_namespace_not_present() {
        whenever(listener.observable).thenReturn(just(
                mapOf(mock<DatasetNamespace>() to mapOf(mock<DatasetId>() to mock<DatasetConfig>()))
        ))

        all(watcher.events) // Ensure there's a subscriber
    }

    private fun coords(namespace: DatasetNamespace, id: DatasetId) = DatasetCoordinates(namespace, id)
}
