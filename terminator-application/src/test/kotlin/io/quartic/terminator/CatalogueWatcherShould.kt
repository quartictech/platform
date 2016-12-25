package io.quartic.terminator

import com.fasterxml.jackson.databind.JavaType
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.catalogue.api.*
import io.quartic.common.websocket.WebsocketListener
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import rx.Observable.just
import java.time.Instant
import java.util.*
import java.util.Collections.emptyMap

class CatalogueWatcherShould {
    private val listener = mock<WebsocketListener<Map<DatasetId, DatasetConfig>>>()
    private val listenerFactory = mock<WebsocketListener.Factory>()
    private val proxy = CatalogueWatcher(listenerFactory)

    @Before
    fun before() {
        whenever(listenerFactory.create<Map<DatasetId, DatasetConfig>>(any<JavaType>())).thenReturn(listener)
    }

    @After
    fun after() {
        proxy.close()
    }

    @Test
    fun expose_returned_ids() {
        @Suppress("USELESS_CAST")
        val terminationId = TerminationIdImpl.of("456") as TerminationId
        val datasets = datasetsWithLocator(TerminatorDatasetLocatorImpl.of(terminationId))
        whenever(listener.observable).thenReturn(just(datasets))

        proxy.start()

        assertThat(proxy.terminationIds, contains(terminationId))
    }

    @Test
    fun ignore_incorrect_types() {
        val datasets = datasetsWithLocator(PostgresDatasetLocatorImpl.of("a", "b", "c", "d"))
        whenever(listener.observable).thenReturn(just(datasets))

        proxy.start()

        assertThat(proxy.terminationIds, empty())
    }

    private fun datasetsWithLocator(locator: DatasetLocator): Map<DatasetId, DatasetConfig> = hashMapOf(DatasetIdImpl.of("123") to DatasetConfigImpl.of(
            DatasetMetadataImpl.of(
                    "foo",
                    "bar",
                    "baz",
                    Optional.of(Instant.now()),
                    Optional.empty<Icon>()
            ),
            locator,
            emptyMap<String, Any>()
    ))
}
