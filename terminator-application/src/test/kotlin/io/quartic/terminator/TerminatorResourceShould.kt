package io.quartic.terminator

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists.newArrayList
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.catalogue.api.TerminationId
import io.quartic.geojson.Feature
import io.quartic.geojson.FeatureCollectionImpl
import io.quartic.geojson.FeatureImpl
import io.quartic.terminator.api.FeatureCollectionWithTerminationId
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.Assert.assertThat
import org.junit.Test
import rx.observers.TestSubscriber
import java.util.*
import java.util.Collections.emptyMap
import javax.ws.rs.NotFoundException

class TerminatorResourceShould {
    private val featureCollection = FeatureCollectionImpl.of(newArrayList(
            FeatureImpl.of(Optional.of("456"), Optional.of(mock()), emptyMap<String, Any>()) as Feature
    ))
    private val terminationId = mock<TerminationId>()
    private val catalogue = mock<CatalogueWatcher>()
    private val resource = TerminatorResource(catalogue)

    @Test
    fun emit_collections_for_things_in_catalogue() {
        whenever(catalogue.terminationIds()).thenReturn(ImmutableSet.of(terminationId))

        val subscriber = TestSubscriber.create<FeatureCollectionWithTerminationId>()
        resource.featureCollections.subscribe(subscriber)
        resource.postToDataset(terminationId, featureCollection)

        assertThat(subscriber.onNextEvents,
                contains(FeatureCollectionWithTerminationId(terminationId, featureCollection)))
    }

    @Test(expected = NotFoundException::class)
    fun block_collections_for_things_not_in_catalogue() {
        resource.postToDataset(terminationId, featureCollection)
    }

    @Test
    fun not_emit_collections_from_before_subscription() {
        whenever(catalogue.terminationIds()).thenReturn(ImmutableSet.of(terminationId))

        val subscriber = TestSubscriber.create<FeatureCollectionWithTerminationId>()
        resource.postToDataset(terminationId, featureCollection)
        resource.featureCollections.subscribe(subscriber)

        assertThat(subscriber.onNextEvents, empty<FeatureCollectionWithTerminationId>())
    }
}
