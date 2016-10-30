package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.quartic.jester.api.*;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.importer.Importer;
import io.quartic.weyl.core.importer.Stuff;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class JesterManagerShould {
    private static class SourceA implements DatasetSource {}
    private static class SourceB implements DatasetSource {}
    private static class SourceC implements DatasetSource {}

    private final JesterService jester = mock(JesterService.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final Stuff stuffA = stuff();
    private final Stuff stuffB = stuff();
    private final Importer importerA = importerOf(stuffA);
    private final Importer importerB = importerOf(stuffB);

    private final Map<Class<? extends DatasetSource>, Function<DatasetSource, Importer>> importerFactories = ImmutableMap.of(
            SourceA.class, source -> importerA,
            SourceB.class, source -> importerB,
            SourceC.class, source -> { throw new RuntimeException("sad times"); }
    );
    private final JesterManager manager = new JesterManager(jester, layerStore, importerFactories);

    @Test
    public void create_and_import_layer_for_new_dataset() throws Exception {
        final TestSubscriber<Stuff> subscriber = TestSubscriber.create();
        when(layerStore.createLayer(any(), any())).thenReturn(subscriber);
        when(jester.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new SourceA())));

        manager.run();

        verify(layerStore).createLayer(LayerId.of("123"), LayerMetadata.of("foo", "bar", Optional.of("baz"), Optional.empty()));
        subscriber.assertValue(stuffA);
    }


    @Test
    public void only_process_each_dataset_once() throws Exception {
        final TestSubscriber<Stuff> subscriber = TestSubscriber.create();
        when(layerStore.createLayer(any(), any())).thenReturn(subscriber);
        when(jester.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new SourceA())));

        manager.run();
        manager.run();

        verify(layerStore, times(1)).createLayer(any(LayerId.class), any(LayerMetadata.class));
    }

    @Test
    public void process_datasets_appearing_later() throws Exception {
        final TestSubscriber<Stuff> subscriberA = TestSubscriber.create();
        final TestSubscriber<Stuff> subscriberB = TestSubscriber.create();
        when(layerStore.createLayer(eq(LayerId.of("123")), any())).thenReturn(subscriberA);
        when(layerStore.createLayer(eq(LayerId.of("456")), any())).thenReturn(subscriberB);
        when(jester.getDatasets())
                .thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new SourceA())))
                .thenReturn(ImmutableMap.of(DatasetId.of("456"), datasetConfig(new SourceB())));

        manager.run();
        manager.run();

        verify(layerStore).createLayer(eq(LayerId.of("123")), any(LayerMetadata.class));
        subscriberA.assertValue(stuffA);
        verify(layerStore).createLayer(eq(LayerId.of("456")), any(LayerMetadata.class));
        subscriberB.assertValue(stuffB);
    }

    @Test
    public void not_propagate_exceptions() throws Exception {
        when(jester.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfig(new SourceC())));

        manager.run();
    }

    private Stuff stuff() {
        return Stuff.of(newArrayList(mock(Feature.class)), emptyList());
    }

    private DatasetConfig datasetConfig(DatasetSource source) {
        return DatasetConfig.of(
                DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                source
        );
    }

    private Importer importerOf(Stuff stuff) {
        final Importer importer = mock(Importer.class);
        when(importer.getObservable()).thenReturn(just(stuff));
        return importer;
    }
}
