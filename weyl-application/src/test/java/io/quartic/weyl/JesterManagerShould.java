package io.quartic.weyl;

import com.google.common.collect.ImmutableMap;
import io.quartic.jester.api.*;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.importer.Importer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.mockito.Mockito.*;

public class JesterManagerShould {
    private static class TestDatasetSourceA implements DatasetSource {}
    private static class TestDatasetSourceB implements DatasetSource {}

    private final JesterService jester = mock(JesterService.class);
    private final LayerStore layerStore = mock(LayerStore.class);
    private final Importer importerA = mock(Importer.class);
    private final Importer importerB = mock(Importer.class);
    private final Map<Class<? extends DatasetSource>, Function<DatasetSource, Importer>> importerFactories = ImmutableMap.of(
            TestDatasetSourceA.class, source -> importerA,
            TestDatasetSourceB.class, source -> importerB
    );

    private final JesterManager manager = new JesterManager(jester, layerStore, importerFactories);


    @Test
    public void create_and_import_layer_for_new_dataset() throws Exception {
        when(jester.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfigA()));

        manager.run();

        verify(layerStore).createLayer(LayerId.of("123"), LayerMetadata.of("foo", "bar", Optional.of("baz"), Optional.empty()));
        verify(layerStore).importToLayer(LayerId.of("123"), importerA);
    }

    @Test
    public void only_process_each_dataset_once() throws Exception {
        when(jester.getDatasets()).thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfigA()));

        manager.run();
        manager.run();

        verify(layerStore, times(1)).createLayer(any(LayerId.class), any(LayerMetadata.class));
        verify(layerStore, times(1)).importToLayer(any(LayerId.class), any(Importer.class));
    }

    @Test
    public void process_datasets_appearing_later() throws Exception {
        when(jester.getDatasets())
                .thenReturn(ImmutableMap.of(DatasetId.of("123"), datasetConfigA()))
                .thenReturn(ImmutableMap.of(DatasetId.of("456"), datasetConfigB()));

        manager.run();
        manager.run();

        verify(layerStore).createLayer(eq(LayerId.of("123")), any(LayerMetadata.class));
        verify(layerStore).importToLayer(any(LayerId.class), eq(importerA));
        verify(layerStore).createLayer(eq(LayerId.of("456")), any(LayerMetadata.class));
        verify(layerStore).importToLayer(any(LayerId.class), eq(importerB));
    }

    private DatasetConfig datasetConfigA() {
        return DatasetConfig.of(
                DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                new TestDatasetSourceA()
        );
    }

    private DatasetConfig datasetConfigB() {
        return DatasetConfig.of(
                DatasetMetadata.of("foo", "bar", "baz", Optional.empty()),
                new TestDatasetSourceB()
        );
    }
}
