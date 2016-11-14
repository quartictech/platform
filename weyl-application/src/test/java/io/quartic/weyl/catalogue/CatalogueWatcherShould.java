package io.quartic.weyl.catalogue;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.client.WebsocketListener;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.quartic.weyl.catalogue.ExtensionParser.EXTENSION_KEY;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class CatalogueWatcherShould {
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
        when(extensionParser.parse(any(), any())).thenReturn(MapDatasetExtension.of(LOCATION_AND_TRACK));
    }

    @After
    public void after() throws Exception {
        watcher.close();
    }

    @Test
    public void create_and_import_layer_for_new_dataset() throws Exception {
        final TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        when(layerStore.createLayer(any(), any(), anyBoolean(), any())).thenReturn(subscriber);
        when(listener.observable()).thenReturn(just(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA()))));

        watcher.start();

        verify(layerStore).createLayer(
                LayerId.of("123"),
                LayerMetadata.of("foo", "bar", Optional.of("baz"), Optional.empty()),
                true,
                LOCATION_AND_TRACK.getLayerView());
        subscriber.assertValue(updateA);
    }

    @Test
    public void only_process_each_dataset_once() throws Exception {
        final TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        when(layerStore.createLayer(any(), any(), anyBoolean(), any())).thenReturn(subscriber);
        when(listener.observable()).thenReturn(just(
                ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA())),
                ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA()))
        ));

        watcher.start();

        verify(layerStore, Mockito.times(1)).createLayer(
                any(),
                any(),
                anyBoolean(),
                any());
    }

    @Test
    public void process_datasets_appearing_later() throws Exception {
        final TestSubscriber<SourceUpdate> subscriberA = TestSubscriber.create();
        final TestSubscriber<SourceUpdate> subscriberB = TestSubscriber.create();
        when(layerStore.createLayer(eq(LayerId.of("123")), any(), anyBoolean(), any())).thenReturn(subscriberA);
        when(layerStore.createLayer(eq(LayerId.of("456")), any(), anyBoolean(), any())).thenReturn(subscriberB);
        when(listener.observable()).thenReturn(just(
                ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA())),
                ImmutableMap.of(DatasetId.of("456"), datasetConfig(new LocatorB()))
        ));

        watcher.start();

        verify(layerStore).createLayer(eq(LayerId.of("123")), any(), eq(true), any());
        subscriberA.assertValue(updateA);
        verify(layerStore).createLayer(eq(LayerId.of("456")), any(), eq(false), any());
        subscriberB.assertValue(updateB);
    }

    @Test
    public void pass_config_fields_to_extension_parser() throws Exception {
        final TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        when(layerStore.createLayer(any(), any(), anyBoolean(), any())).thenReturn(subscriber);
        when(listener.observable()).thenReturn(just(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA()))));

        watcher.start();


        verify(extensionParser).parse("foo", ImmutableMap.of(EXTENSION_KEY, extension()));
    }

    private SourceUpdate createUpdate() {
        return SourceUpdate.of(Lists.newArrayList(mock(Feature.class)), emptyList());
    }

    private DatasetConfig datasetConfig(DatasetLocator source) {
        return DatasetConfig.of(
                DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                source,
                ImmutableMap.of(EXTENSION_KEY, extension())
        );
    }

    private MapDatasetExtension extension() {
        return MapDatasetExtension.of(LOCATION_AND_TRACK);
    }

    private Source importerOf(SourceUpdate update, boolean indexable) {
        final Source source = mock(Source.class);
        when(source.observable()).thenReturn(just(update));
        when(source.indexable()).thenReturn(indexable);
        return source;
    }
}
