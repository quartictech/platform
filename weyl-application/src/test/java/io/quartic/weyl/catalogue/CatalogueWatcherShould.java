package io.quartic.weyl.catalogue;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.WebsocketListener;
import io.quartic.weyl.core.SourceDescriptor;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.test.CollectionUtils.entry;
import static io.quartic.common.test.CollectionUtils.map;
import static io.quartic.weyl.catalogue.ExtensionParser.EXTENSION_KEY;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class CatalogueWatcherShould {

    private static final AttributeName TITLE_ATTRIBUTE = AttributeNameImpl.of("title_attr");
    private static final AttributeName IMAGE_ATTRIBUTE = AttributeNameImpl.of("image_attr");
    private static final AttributeName[] BLESSED_ATTRIBUTES = { AttributeNameImpl.of("cool_attr"), AttributeNameImpl.of("slick_attr") };

    private static class LocatorA implements DatasetLocator {}
    private static class LocatorB implements DatasetLocator {}

    private final WebsocketListener<Map<DatasetId, DatasetConfig>> listener = mock(WebsocketListener.class);
    private final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
    private final ExtensionParser extensionParser = mock(ExtensionParser.class);
    private final SourceUpdate updateA = createUpdate();
    private final SourceUpdate updateB = createUpdate();
    private final Source sourceA = importerOf(updateA, true);
    private final Source sourceB = importerOf(updateB, false);
    private final String rawExtension = "raw";

    private final Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories = ImmutableMap.of(
            LocatorA.class, config -> sourceA,
            LocatorB.class, config -> sourceB
    );

    private final CatalogueWatcher watcher = CatalogueWatcher.builder()
            .listenerFactory(listenerFactory)
            .sourceFactories(sourceFactories)
            .scheduler(Schedulers.immediate()) // Force onto same thread for synchronous behaviour
            .extensionParser(extensionParser)
            .build();

    @Before
    public void before() throws Exception {
        when(listenerFactory.create(any(JavaType.class))).thenReturn((WebsocketListener)listener);
        when(extensionParser.parse(any(), any())).thenReturn(extension());
    }

    @Test
    public void create_and_import_layer_for_new_dataset() throws Exception {
        when(listener.observable()).thenReturn(just(
                map(entry(DatasetIdImpl.of("123"), datasetConfig(new LocatorA())))
        ));

        final SourceDescriptor descriptor = collectSourceDescriptors().get(0);
        assertThat(descriptor.id(), equalTo(LayerIdImpl.of("123")));
        assertThat(descriptor.metadata(), equalTo(LayerMetadataImpl.of("foo", "bar", Optional.of("baz"), Optional.empty())));
        assertThat(descriptor.view(), equalTo(LOCATION_AND_TRACK.getLayerView()));
        assertThat(descriptor.schema(), equalTo(AttributeSchemaImpl.builder()
                .titleAttribute(TITLE_ATTRIBUTE)
                .imageAttribute(IMAGE_ATTRIBUTE)
                .blessedAttribute(BLESSED_ATTRIBUTES)
                .build()));
        assertThat(descriptor.indexable(), equalTo(true));
        assertThat(descriptor.updates().toBlocking().single(), equalTo(updateA));
    }

    @Test
    public void only_process_each_dataset_once() throws Exception {
        when(listener.observable()).thenReturn(just(
                map(entry(DatasetIdImpl.of("123"), datasetConfig(new LocatorA()))),
                map(entry(DatasetIdImpl.of("123"), datasetConfig(new LocatorA())))
        ));

        assertThat(collectSourceDescriptors(), hasSize(1));
    }

    @Test
    public void process_datasets_appearing_later() throws Exception {
        when(listener.observable()).thenReturn(just(
                map(entry(DatasetIdImpl.of("123"), datasetConfig(new LocatorA()))),
                map(entry(DatasetIdImpl.of("456"), datasetConfig(new LocatorB())))
        ));

        assertThat(collectSourceDescriptors().stream().map(SourceDescriptor::id).collect(toList()),
                Matchers.contains(LayerIdImpl.of("123"), LayerIdImpl.of("456")));
    }

    @Test
    public void pass_config_fields_to_extension_parser() throws Exception {
        when(listener.observable()).thenReturn(just(
                map(entry(DatasetIdImpl.of("123"), datasetConfig(new LocatorA())))
        ));

        collectSourceDescriptors();

        verify(extensionParser).parse("foo", ImmutableMap.of(EXTENSION_KEY, rawExtension));
    }

    private List<SourceDescriptor> collectSourceDescriptors() {
        TestSubscriber<SourceDescriptor> sub = TestSubscriber.create();
        watcher.sources().subscribe(sub);
        sub.awaitTerminalEvent();
        return sub.getOnNextEvents();
    }

    private SourceUpdate createUpdate() {
        return SourceUpdateImpl.of(newArrayList(mock(NakedFeature.class)));
    }

    private DatasetConfig datasetConfig(DatasetLocator source) {
        return DatasetConfigImpl.of(
                DatasetMetadataImpl.of("foo", "bar", "baz", Optional.empty()),
                source,
                ImmutableMap.of(EXTENSION_KEY, rawExtension)
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

    private Source importerOf(SourceUpdate update, boolean indexable) {
        final Source source = mock(Source.class);
        when(source.observable()).thenReturn(just(update));
        when(source.indexable()).thenReturn(indexable);
        return source;
    }
}
