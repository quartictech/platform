package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class CatalogueManagerShould {
    private static class LocatorA implements DatasetLocator {}
    private static class LocatorB implements DatasetLocator {}
    private static class LocatorC implements DatasetLocator {}

    private final CatalogueService catalogue = mock(CatalogueService.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final SourceUpdate updateA = createUpdate();
    private final SourceUpdate updateB = createUpdate();
    private final Source sourceA = importerOf(updateA, true);
    private final Source sourceB = importerOf(updateB, false);

    private final Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> importerFactories = ImmutableMap.of(
            LocatorA.class, config -> sourceA,
            LocatorB.class, config -> sourceB,
            LocatorC.class, config -> { throw new RuntimeException("sad times"); }
    );
    private final CatalogueManager manager = new CatalogueManager(catalogue, layerStore, importerFactories, Schedulers.immediate()); // Force onto same thread for synchronous behaviour

    @Test
    public void create_and_import_layer_for_new_dataset() throws Exception {
        final TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        when(layerStore.createLayer(any(), any(), anyBoolean(), any())).thenReturn(subscriber);
        when(catalogue.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA())));

        manager.run();

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
        when(catalogue.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA())));

        manager.run();
        manager.run();

        verify(layerStore, times(1)).createLayer(
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
        when(catalogue.getDatasets())
                .thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorA())))
                .thenReturn(ImmutableMap.of(DatasetId.of("456"), datasetConfig(new LocatorB())));

        manager.run();
        manager.run();

        verify(layerStore).createLayer(eq(LayerId.of("123")), any(), eq(true), any());
        subscriberA.assertValue(updateA);
        verify(layerStore).createLayer(eq(LayerId.of("456")), any(), eq(false), any());
        subscriberB.assertValue(updateB);
    }

    @Test
    public void not_propagate_exceptions() throws Exception {
        when(catalogue.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new LocatorC())));

        manager.run();
    }

    private SourceUpdate createUpdate() {
        return SourceUpdate.of(newArrayList(mock(Feature.class)), emptyList());
    }

    private DatasetConfig datasetConfig(DatasetLocator source) {
        return DatasetConfig.of(
                DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                source
        );
    }

    private Source importerOf(SourceUpdate update, boolean indexable) {
        final Source source = mock(Source.class);
        when(source.getObservable()).thenReturn(just(update));
        when(source.indexable()).thenReturn(indexable);
        when(source.viewType()).thenReturn(LOCATION_AND_TRACK);
        return source;
    }
}
