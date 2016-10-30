package io.quartic.weyl;

import com.google.common.collect.Maps;
import io.quartic.jester.api.*;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.function.Function;

public class JesterManager implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(JesterManager.class);

    private final Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();
    private final Map<Class<? extends DatasetSource>, Function<DatasetSource, Source>> sourceFactories;
    private final JesterService jester;
    private final LayerStore layerStore;


    public JesterManager(
            JesterService jester,
            LayerStore layerStore,
            Map<Class<? extends DatasetSource>, Function<DatasetSource, Source>> sourceFactories) {
        this.jester = jester;
        this.layerStore = layerStore;
        this.sourceFactories = sourceFactories;
    }

    @Override
    public void run() {
        // TODO: exception handling
        final Map<DatasetId, DatasetConfig> datasets = jester.getDatasets();
        datasets.entrySet().stream()
                .filter(e -> !this.datasets.containsKey(e.getKey()))
                .forEach(e -> createAndImportLayer(e.getKey(), e.getValue()));
        // TODO: explicitly delete old layers
        this.datasets.clear();
        this.datasets.putAll(datasets);
    }

    private void createAndImportLayer(DatasetId id, DatasetConfig config) {
        try {
            final Function<DatasetSource, Source> func = sourceFactories.get(config.source().getClass());
            if (func == null) {
                throw new IllegalArgumentException("Unrecognised config type " + config.source().getClass());
            }

            final Source source = func.apply(config.source());

            final LayerId layerId = LayerId.of(id.uid());
            final Subscriber<SourceUpdate> subscriber = layerStore.createLayer(layerId, datasetMetadataFrom(config.metadata()));
            source.getObservable().subscribeOn(Schedulers.computation()).subscribe(subscriber);   // TODO: the scheduler should be chosen by the specific source
        } catch (Exception e) {
            LOG.error("Error creating layer for dataset " + id, e);
        }
    }

    // TODO: do we really need LayerMetadata to be distinct from DatasetMetadata?
    private LayerMetadata datasetMetadataFrom(DatasetMetadata metadata) {
        return LayerMetadata.builder()
                .name(metadata.name())
                .description(metadata.description())
                .attribution(metadata.attribution())
                .icon(metadata.icon())
                .build();
    }
}
