package io.quartic.weyl.core.source;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.api.*;
import io.quartic.common.test.rx.ObservableInterceptor;
import io.quartic.weyl.core.catalogue.CatalogueEvent;
import io.quartic.weyl.core.catalogue.CatalogueEventImpl;
import io.quartic.weyl.core.model.*;
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
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SourceManagerShould {

    private static final AttributeName TITLE_ATTRIBUTE = AttributeNameImpl.of("title_attr");
    private static final AttributeName IMAGE_ATTRIBUTE = AttributeNameImpl.of("image_attr");
    private static final AttributeName[] BLESSED_ATTRIBUTES = { AttributeNameImpl.of("cool_attr"), AttributeNameImpl.of("slick_attr") };

    private static class LocatorA implements DatasetLocator {}
    private static class LocatorB implements DatasetLocator {}

    private final PublishSubject<CatalogueEvent> catalogueEvents = PublishSubject.create();
    private final PublishSubject<SourceUpdate> sourceUpdatesA = PublishSubject.create();
    private final PublishSubject<SourceUpdate> sourceUpdatesB = PublishSubject.create();
    private final ObservableInterceptor<SourceUpdate> interceptor = ObservableInterceptor.create(sourceUpdatesA);

    private final Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories = ImmutableMap.of(
            LocatorA.class, config -> sourceOf(interceptor.observable(), true),
            LocatorB.class, config -> sourceOf(sourceUpdatesB, false)
    );

    private final ExtensionParser extensionParser = mock(ExtensionParser.class);

    private final SourceManager manager = SourceManagerImpl.builder()
            .catalogueEvents(catalogueEvents)
            .sourceFactories(sourceFactories)
            .extensionParser(extensionParser)
            .scheduler(Schedulers.immediate()) // Force onto same thread for synchronous behaviour
            .build();

    private final TestSubscriber<SourceDescriptor> sub = TestSubscriber.create();
    private final Map<LayerId, TestSubscriber<SourceUpdate>> updateSubscribers = Maps.newHashMap();

    @Before
    public void before() throws Exception {
        when(extensionParser.parse(any(), any())).thenReturn(extension());
        manager.sources()
                .doOnNext(desc -> {
                    // This mechanism allows us to capture source updates in a non-blocking way
                    final TestSubscriber<SourceUpdate> updateSubscriber = TestSubscriber.create();
                    desc.updates().subscribe(updateSubscriber);
                    updateSubscribers.put(desc.id(), updateSubscriber);
                })
                .subscribe(sub);
    }

    @Test
    public void create_layer_on_create_event() throws Exception {
        final SourceUpdate sourceUpdate = mock(SourceUpdate.class);

        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        sourceUpdatesA.onNext(sourceUpdate);
        sourceUpdatesA.onCompleted();
        catalogueEvents.onCompleted();

        final SourceDescriptor descriptor = collectedSourceDescriptors().get(0);
        assertThat(descriptor.id(), equalTo(LayerIdImpl.of("123")));
        assertThat(descriptor.metadata(), equalTo(LayerMetadataImpl.of("foo", "blah", Optional.of("quartic"), Optional.empty())));
        assertThat(descriptor.view(), equalTo(LOCATION_AND_TRACK.getLayerView()));
        assertThat(descriptor.schema(), equalTo(AttributeSchemaImpl.builder()
                .titleAttribute(TITLE_ATTRIBUTE)
                .imageAttribute(IMAGE_ATTRIBUTE)
                .blessedAttribute(BLESSED_ATTRIBUTES)
                .build()));
        assertThat(descriptor.indexable(), equalTo(true));
        assertThat(collectedUpdateSequenceFor("123"), contains(sourceUpdate));
    }

    @Test
    public void complete_source_observable_on_delete_event() throws Exception {
        final SourceUpdate beforeDeletion = mock(SourceUpdate.class);
        final SourceUpdate afterDeletion = mock(SourceUpdate.class);

        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        sourceUpdatesA.onNext(beforeDeletion);
        catalogueEvents.onNext(event(DELETE, "123", "foo", new LocatorA()));
        sourceUpdatesA.onNext(afterDeletion);
        sourceUpdatesA.onCompleted();
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

        assertThat(collectedSourceDescriptors().stream().map(SourceDescriptor::id).collect(toList()),
                contains(LayerIdImpl.of("123"), LayerIdImpl.of("456")));
    }

    @Test
    public void pass_config_fields_to_extension_parser() throws Exception {
        catalogueEvents.onNext(event(CREATE, "123", "foo", new LocatorA()));
        catalogueEvents.onCompleted();

        collectedSourceDescriptors();

        verify(extensionParser).parse("foo", ImmutableMap.of(EXTENSION_KEY, "raw"));
    }

    private List<SourceUpdate> collectedUpdateSequenceFor(String layerId) {
        return updateSubscribers.get(LayerIdImpl.of(layerId)).getOnNextEvents();
    }

    private List<SourceDescriptor> collectedSourceDescriptors() {
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
                .titleAttribute(TITLE_ATTRIBUTE)
                .imageAttribute(IMAGE_ATTRIBUTE)
                .blessedAttribute(BLESSED_ATTRIBUTES)
                .build();
    }

    private Source sourceOf(Observable<SourceUpdate> updates, boolean indexable) {
        final Source source = mock(Source.class);
        when(source.observable()).thenReturn(updates);
        when(source.indexable()).thenReturn(indexable);
        return source;
    }
}
