package io.quartic.weyl.catalogue;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.WebsocketListener;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.catalogue.ExtensionParser.EXTENSION_KEY;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static rx.Observable.just;
import static rx.functions.Actions.empty;

public class CatalogueWatcherShould {

    private static final AttributeName TITLE_ATTRIBUTE = AttributeNameImpl.of("title_attr");
    private static final AttributeName IMAGE_ATTRIBUTE = AttributeNameImpl.of("image_attr");
    private static final AttributeName[] BLESSED_ATTRIBUTES = { AttributeNameImpl.of("cool_attr"), AttributeNameImpl.of("slick_attr") };

    private static class LocatorA implements DatasetLocator {}
    private static class LocatorB implements DatasetLocator {}
    private static class LocatorC implements DatasetLocator {}

    private final WebsocketListener<Map<DatasetId, DatasetConfig>> listener = mock(WebsocketListener.class);
    private final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
    private final ExtensionParser extensionParser = mock(ExtensionParser.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final SourceUpdate updateA = createUpdate();
    private final SourceUpdate updateB = createUpdate();
    private final Source sourceA = importerOf(updateA, true);
    private final Source sourceB = importerOf(updateB, false);
    private final String rawExtension = "raw";

    private final Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories = ImmutableMap.of(
            LocatorA.class, config -> sourceA,
            LocatorB.class, config -> sourceB,
            LocatorC.class, config -> { throw new RuntimeException("sad times"); }
    );

    private final CatalogueWatcher watcher = CatalogueWatcher.builder()
            .listenerFactory(listenerFactory)
            .sourceFactories(sourceFactories)
            .layerStore(layerStore)
            .scheduler(Schedulers.immediate()) // Force onto same thread for synchronous behaviour
            .extensionParser(extensionParser)
            .build();

    @Before
    public void before() throws Exception {
        when(listenerFactory.create(any(JavaType.class))).thenReturn((WebsocketListener)listener);
        when(extensionParser.parse(any(), any())).thenReturn(extension());
    }

    @After
    public void after() throws Exception {
        watcher.close();
    }

    @Test
    public void create_and_import_layer_for_new_dataset() throws Exception {
        final Action1<SourceUpdate> action = mock(Action1.class);
        whenCreateLayerThenReturn("123", action);
        when(listener.observable()).thenReturn(just(ImmutableMap.of(DatasetIdImpl.of("123"), datasetConfig(new LocatorA()))));

        watcher.start();

        verify(layerStore).createLayer(
                LayerIdImpl.of("123"),
                LayerMetadataImpl.of("foo", "bar", Optional.of("baz"), Optional.empty()),
                LOCATION_AND_TRACK.getLayerView(),
                AttributeSchemaImpl.builder()
                        .titleAttribute(TITLE_ATTRIBUTE)
                        .imageAttribute(IMAGE_ATTRIBUTE)
                        .blessedAttribute(BLESSED_ATTRIBUTES)
                        .build(),
                true
        );
        verify(action).call(updateA);
    }

    @Test
    public void only_process_each_dataset_once() throws Exception {
        whenCreateLayerThenReturn("123", empty());
        when(listener.observable()).thenReturn(just(
                ImmutableMap.of(DatasetIdImpl.of("123"), datasetConfig(new LocatorA())),
                ImmutableMap.of(DatasetIdImpl.of("123"), datasetConfig(new LocatorA()))
        ));

        watcher.start();

        verify(layerStore, times(1)).createLayer(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    public void process_datasets_appearing_later() throws Exception {
        final Action1<SourceUpdate> actionA = mock(Action1.class);
        final Action1<SourceUpdate> actionB = mock(Action1.class);
        whenCreateLayerThenReturn("123", actionA);
        whenCreateLayerThenReturn("456", actionB);
        when(listener.observable()).thenReturn(just(
                ImmutableMap.of(DatasetIdImpl.of("123"), datasetConfig(new LocatorA())),
                ImmutableMap.of(DatasetIdImpl.of("456"), datasetConfig(new LocatorB()))
        ));

        watcher.start();

        verify(layerStore).createLayer(eq(LayerIdImpl.of("123")), any(), any(), any(), eq(true));
        verify(actionA).call(updateA);
        verify(layerStore).createLayer(eq(LayerIdImpl.of("456")), any(), any(), any(), eq(false));
        verify(actionB).call(updateB);
    }

    @Test
    public void pass_config_fields_to_extension_parser() throws Exception {
        whenCreateLayerThenReturn("123", empty());
        when(listener.observable()).thenReturn(just(ImmutableMap.of(DatasetIdImpl.of("123"), datasetConfig(new LocatorA()))));

        watcher.start();

        verify(extensionParser).parse("foo", ImmutableMap.of(EXTENSION_KEY, rawExtension));
    }

    private void whenCreateLayerThenReturn(String layerId, Action1<SourceUpdate> action) {
        when(layerStore.createLayer(eq(LayerIdImpl.of(layerId)), any(), any(), any(), anyBoolean())).thenReturn(action);
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
