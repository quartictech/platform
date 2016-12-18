package io.quartic.weyl.core.source;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetConfigImpl;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadataImpl;
import io.quartic.common.test.rx.ObservableInterceptor;
import io.quartic.weyl.core.catalogue.CatalogueEvent;
import io.quartic.weyl.core.catalogue.CatalogueEventImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerIdImpl;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.model.MapDatasetExtensionImpl;
import io.quartic.weyl.core.model.StaticSchemaImpl;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.CREATE;
import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.DELETE;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static io.quartic.weyl.core.source.ExtensionParser.EXTENSION_KEY;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceManagerShould {

    private static final AttributeName TITLE_ATTRIBUTE = AttributeNameImpl.of("title_attr");
    private static final AttributeName IMAGE_ATTRIBUTE = AttributeNameImpl.of("image_attr");
    private static final AttributeName[] BLESSED_ATTRIBUTES = { AttributeNameImpl.of("cool_attr"), AttributeNameImpl.of("slick_attr") };

    private static class LocatorA implements DatasetLocator {}
    private static class LocatorB implements DatasetLocator {}

    private final PublishSubject<CatalogueEvent> catalogueEvents = PublishSubject.create();
    private final PublishSubject<LayerUpdate> layerUpdatesA = PublishSubject.create();
    private final PublishSubject<LayerUpdate> layerUpdatesB = PublishSubject.create();
    private final ObservableInterceptor<LayerUpdate> interceptor = ObservableInterceptor.create(layerUpdatesA);

    private final Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories = ImmutableMap.of(
            LocatorA.class, config -> sourceOf(interceptor.observable(), true),
            LocatorB.class, config -> sourceOf(layerUpdatesB, false)
    );

    private final ExtensionParser extensionParser = mock(ExtensionParser.class);

    private final SourceManager manager = SourceManagerImpl.builder()
            .catalogueEvents(catalogueEvents)
            .sourceFactories(sourceFactories)
            .extensionParser(extensionParser)
            .scheduler(Schedulers.immediate()) // Force onto same thread for synchronous behaviour
            .build();

    private final TestSubscriber<LayerPopulator> sub = TestSubscriber.create();
    private final Map<LayerId, TestSubscriber<LayerUpdate>> updateSubscribers = Maps.newHashMap();

    @Before
    public void before() throws Exception {
        when(extensionParser.parse(any(), any())).thenReturn(extension());
        manager.layerPopulators()
                .doOnNext(populator -> {
                    // This mechanism allows us to capture source updates in a non-blocking way
                    final TestSubscriber<LayerUpdate> updateSubscriber = TestSubscriber.create();
                    final LayerSpec spec = populator.spec(emptyList());
                    populator.updates(emptyList()).subscribe(updateSubscriber);
                    updateSubscribers.put(spec.id(), updateSubscriber);
                })
                .subscribe(sub);
    }

    @Test
    public void create_layer_on_create_event() throws Exception {
        final LayerUpdate layerUpdate = mock(LayerUpdate.class);

        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        layerUpdatesA.onNext(layerUpdate);
        layerUpdatesA.onCompleted();
        catalogueEvents.onCompleted();

        final LayerPopulator populator = collectedLayerPopulators().get(0);
        assertThat(populator.spec(emptyList()), equalTo(LayerSpecImpl.of(
                LayerIdImpl.of("123"),
                LayerMetadataImpl.of("foo", "blah", Optional.of("quartic"), Optional.empty()),
                LOCATION_AND_TRACK.getLayerView(),
                staticSchema(),
                true
        )));
        assertThat(populator.dependencies(), empty());
        assertThat(collectedUpdateSequenceFor("123"), contains(layerUpdate));
    }

    @Test
    public void complete_source_observable_on_delete_event() throws Exception {
        final LayerUpdate beforeDeletion = mock(LayerUpdate.class);
        final LayerUpdate afterDeletion = mock(LayerUpdate.class);

        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        layerUpdatesA.onNext(beforeDeletion);
        catalogueEvents.onNext(event(DELETE, "123", "foo", new LocatorA()));
        layerUpdatesA.onNext(afterDeletion);
        layerUpdatesA.onCompleted();
        catalogueEvents.onCompleted();

        assertThat(collectedUpdateSequenceFor("123"), contains(beforeDeletion));    // But not afterDeletion
    }

    @Test
    public void unsubscribe_from_upstream_source_on_delete_event() throws Exception {
        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        catalogueEvents.onNext(event(DELETE, "123", "foo", new LocatorA()));

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    @Test
    public void process_datasets_appearing_later() throws Exception {
        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        catalogueEvents.onNext(event(CREATE, "456", "foo", new LocatorB()));
        catalogueEvents.onCompleted();

        assertThat(collectedLayerPopulators().stream().map(p -> p.spec(emptyList()).id()).collect(toList()),
                contains(LayerIdImpl.of("123"), LayerIdImpl.of("456")));
    }

    @Test
    public void pass_config_fields_to_extension_parser() throws Exception {
        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        catalogueEvents.onCompleted();

        collectedLayerPopulators();

        verify(extensionParser).parse("foo", ImmutableMap.of(EXTENSION_KEY, "raw"));
    }

    private List<LayerUpdate> collectedUpdateSequenceFor(String layerId) {
        return updateSubscribers.get(LayerIdImpl.of(layerId)).getOnNextEvents();
    }

    private List<LayerPopulator> collectedLayerPopulators() {
        sub.awaitTerminalEvent();
        return sub.getOnNextEvents();
    }

    private CatalogueEvent event(CatalogueEvent.Type type, String id, String name, DatasetLocator locator) {
        return CatalogueEventImpl.of(type, DatasetId.fromString(id), datasetConfig(name, locator));
    }

    private DatasetConfig datasetConfig(String name, DatasetLocator source) {
        return DatasetConfigImpl.of(
                DatasetMetadataImpl.of(name, "blah", "quartic", Optional.empty()),
                source,
                ImmutableMap.of(EXTENSION_KEY, "raw")
        );
    }

    private MapDatasetExtension extension() {
        return MapDatasetExtensionImpl.builder()
                .viewType(LOCATION_AND_TRACK)
                .staticSchema(staticSchema())
                .build();
    }

    private StaticSchemaImpl staticSchema() {
        return StaticSchemaImpl.builder()
                .titleAttribute(TITLE_ATTRIBUTE)
                .imageAttribute(IMAGE_ATTRIBUTE)
                .blessedAttribute(BLESSED_ATTRIBUTES)
                .build();
    }

    private Source sourceOf(Observable<LayerUpdate> updates, boolean indexable) {
        final Source source = mock(Source.class);
        when(source.observable()).thenReturn(updates);
        when(source.indexable()).thenReturn(indexable);
        return source;
    }
}
