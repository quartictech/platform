package io.quartic.catalogue

import com.fasterxml.jackson.databind.type.MapType
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.common.rx.all
import io.quartic.common.websocket.WebsocketListener
import org.hamcrest.Matchers.contains
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import rx.Observable.just

class CatalogueWatcherShould {
    private val listener = mock<WebsocketListener<Map<DatasetId, DatasetConfig>>>()
    private val listenerFactory = mock<WebsocketListener.Factory>()

    private val watcher = CatalogueWatcher(listenerFactory)

    @Before
    fun before() {
        whenever(listenerFactory.create<Map<DatasetId, DatasetConfig>>(any<MapType>())).thenReturn(listener)
    }

    @Test
    fun emit_creation_event_when_dataset_appears() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(mapOf(id to config)))

        assertThat(all(watcher.events), contains(CatalogueEvent(CREATE, id, config)))
    }

    @Test
    fun emit_deletion_event_when_dataset_appears() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(
                mapOf(id to config),
                emptyMap()   // Gone!
        ))

        assertThat(all(watcher.events), contains(
                CatalogueEvent(CREATE, id, config),
                CatalogueEvent(DELETE, id, config)
        ))
    }

    @Test
    fun not_emit_creation_event_when_dataset_persists() {
        val id = mock<DatasetId>()
        val config = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(
                mapOf(id to config),
                mapOf(id to config)  // Again
        ))

        assertThat(all(watcher.events), contains(CatalogueEvent(CREATE, id, config)))
    }

    @Test
    fun emit_events_for_multiple_independent_datasets() {
        val idA = mock<DatasetId>()
        val idB = mock<DatasetId>()
        val configA = mock<DatasetConfig>()
        val configB = mock<DatasetConfig>()

        whenever(listener.observable).thenReturn(just(
                mapOf(idA to configA),
                mapOf(idA to configA, idB to configB),
                mapOf(idB to configB)
        ))

        assertThat(all(watcher.events), contains(
                CatalogueEvent(CREATE, idA, configA),
                CatalogueEvent(CREATE, idB, configB),
                CatalogueEvent(DELETE, idA, configA)
        ))
    }
}
