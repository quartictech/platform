package io.quartic.weyl.core.source

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.CatalogueEvent
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.model.*
import io.quartic.common.test.rx.Interceptor
import io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK
import io.quartic.weyl.core.model.*
import io.quartic.weyl.core.source.ExtensionCodec.Companion.EXTENSION_KEY
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.time.Instant
import java.util.*
import java.util.Collections.emptyList
import java.util.function.Function

class SourceManagerShould {

    private class LocatorA : DatasetLocator
    private class LocatorB : DatasetLocator

    private val catalogueEvents = PublishSubject.create<CatalogueEvent>()
    private val layerUpdatesA = PublishSubject.create<LayerUpdate>()
    private val layerUpdatesB = PublishSubject.create<LayerUpdate>()
    private val interceptor = Interceptor<LayerUpdate>()

    private val sourceFactory = Function { config: DatasetConfig ->
        when (config.locator) {
            is LocatorA -> Optional.of(sourceOf(layerUpdatesA.compose(interceptor), true))
            is LocatorB -> Optional.of(sourceOf(layerUpdatesB, false))
            else -> Optional.empty()
        }
    }

    private val extensionCodec = mock<ExtensionCodec>()

    private val manager = SourceManager(
            catalogueEvents,
            sourceFactory,
            Schedulers.immediate(), // Force onto same thread for synchronous behaviour
            extensionCodec
    )

    private val sub = TestSubscriber.create<LayerPopulator>()
    private val updateSubscribers = mutableMapOf<LayerId, TestSubscriber<LayerUpdate>>()

    @Before
    fun before() {
        whenever(extensionCodec.decode(any(), any())).thenReturn(extension())
        manager.layerPopulators
                .doOnNext { populator ->
                    // This mechanism allows us to capture source updates in a non-blocking way
                    val updateSubscriber = TestSubscriber.create<LayerUpdate>()
                    val spec = populator.spec(emptyList())
                    populator.updates(emptyList()).subscribe(updateSubscriber)
                    updateSubscribers.put(spec.id, updateSubscriber)
                }
                .subscribe(sub)
    }

    @Test
    fun create_layer_on_create_event() {
        val layerUpdate = mock<LayerUpdate>()

        catalogueEvents.onNext(event(CREATE, coords(DatasetId("123")), "foo", LocatorA()))
        layerUpdatesA.onNext(layerUpdate)
        layerUpdatesA.onCompleted()
        catalogueEvents.onCompleted()

        val populator = collectedLayerPopulators()[0]
        assertThat(populator.spec(emptyList()), equalTo(LayerSpec(
                layerIdFor("123"),
                LayerMetadata("foo", "blah", "quartic", Instant.EPOCH),
                LOCATION_AND_TRACK.layerView,
                staticSchema(),
                true
        )))
        assertThat(populator.dependencies(), empty())
        assertThat(collectedUpdateSequenceFor("123"), contains(layerUpdate))
    }

    @Test
    fun complete_source_observable_on_delete_event() {
        val beforeDeletion = mock<LayerUpdate>()
        val afterDeletion = mock<LayerUpdate>()

        catalogueEvents.onNext(event(CREATE, coords(DatasetId("123")), "foo", LocatorA()))
        layerUpdatesA.onNext(beforeDeletion)
        catalogueEvents.onNext(event(DELETE, coords(DatasetId("123")), "foo", LocatorA()))
        layerUpdatesA.onNext(afterDeletion)
        layerUpdatesA.onCompleted()
        catalogueEvents.onCompleted()

        assertThat(collectedUpdateSequenceFor("123"), contains(beforeDeletion))    // But not afterDeletion
    }

    @Test
    fun unsubscribe_from_upstream_source_on_delete_event() {
        catalogueEvents.onNext(event(CREATE, coords(DatasetId("123")), "foo", LocatorA()))
        catalogueEvents.onNext(event(DELETE, coords(DatasetId("123")), "foo", LocatorA()))

        assertThat(interceptor.unsubscribed, equalTo(true))
    }

    @Test
    fun process_datasets_appearing_later() {
        catalogueEvents.onNext(event(CREATE, coords(DatasetId("123")), "foo", LocatorA()))
        catalogueEvents.onNext(event(CREATE, coords(DatasetId("456")), "foo", LocatorB()))
        catalogueEvents.onCompleted()

        assertThat(collectedLayerPopulators().map { p -> p.spec(emptyList()).id },
                contains(layerIdFor("123"), layerIdFor("456")))
    }

    @Test
    fun pass_config_fields_to_extension_parser() {
        catalogueEvents.onNext(event(CREATE, coords(DatasetId("123")), "foo", LocatorA()))
        catalogueEvents.onCompleted()

        collectedLayerPopulators()

        verify(extensionCodec).decode("foo", mapOf(EXTENSION_KEY to "raw"))
    }

    @Test
    fun ignore_entries_without_parsable_extensions() {
        whenever(extensionCodec.decode(any(), any())).thenReturn(null)

        catalogueEvents.onNext(event(CREATE, coords(DatasetId("123")), "foo", LocatorA()))
        catalogueEvents.onCompleted()

        assertThat(collectedLayerPopulators(), empty())
    }

    private fun coords(id: DatasetId) = DatasetCoordinates(NAMESPACE, id)

    private fun collectedUpdateSequenceFor(datasetId: String) = updateSubscribers[layerIdFor(datasetId)]!!.onNextEvents

    private fun collectedLayerPopulators(): List<LayerPopulator> {
        sub.awaitTerminalEvent()
        return sub.onNextEvents
    }

    private fun event(type: CatalogueEvent.Type, coords: DatasetCoordinates, name: String, locator: DatasetLocator) = CatalogueEvent(
            type,
            coords,
            datasetConfig(name, locator)
    )

    private fun datasetConfig(name: String, source: DatasetLocator) = DatasetConfig(
            DatasetMetadata(name, "blah", "quartic", Instant.EPOCH),
            source,
            mapOf(EXTENSION_KEY to "raw")
    )

    private fun extension() = MapDatasetExtension(staticSchema(), LOCATION_AND_TRACK)

    private fun staticSchema() = StaticSchema(TITLE_ATTRIBUTE, null, IMAGE_ATTRIBUTE, BLESSED_ATTRIBUTES)

    private fun sourceOf(updates: Observable<LayerUpdate>, indexable: Boolean) = mock<Source> {
        on { observable() } doReturn(updates)
        on { indexable() } doReturn(indexable)
    }

    private fun layerIdFor(datasetId: String) = LayerId("(${NAMESPACE.namespace},$datasetId)")

    companion object {
        private val TITLE_ATTRIBUTE = AttributeName("title_attr")
        private val IMAGE_ATTRIBUTE = AttributeName("image_attr")
        private val BLESSED_ATTRIBUTES = listOf(AttributeName("cool_attr"), AttributeName("slick_attr"))
        private val NAMESPACE = DatasetNamespace("my-namespace")
    }
}
