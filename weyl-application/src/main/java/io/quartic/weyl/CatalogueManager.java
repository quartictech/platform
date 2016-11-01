package io.quartic.weyl;

import com.google.common.collect.Maps;
import io.quartic.catalogue.api.*;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Scheduler;
import rx.Subscriber;

import java.util.Map;
import java.util.function.Function;

public class CatalogueManager implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueManager.class);

    private final Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();
    private final Map<Class<? extends DatasetLocator>, Function<DatasetLocator, Source>> sourceFactories;
    private final CatalogueService catalogue;
    private final LayerStore layerStore;
    private final Scheduler scheduler;


    public CatalogueManager(
            CatalogueService catalogue,
            LayerStore layerStore,
            Map<Class<? extends DatasetLocator>, Function<DatasetLocator, Source>> sourceFactories,
            Scheduler scheduler) {
        this.catalogue = catalogue;
        this.layerStore = layerStore;
        this.sourceFactories = sourceFactories;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        // TODO: this is fairly ghetto - we rely on exception handling in Scheduler as our retry policy (no backoff, etc.)
        final Map<DatasetId, DatasetConfig> datasets = catalogue.getDatasets();
        datasets.entrySet().stream()
                .filter(e -> !this.datasets.containsKey(e.getKey()))
                .forEach(e -> createAndImportLayer(e.getKey(), e.getValue()));
        // TODO: explicitly delete old layers
        this.datasets.clear();
        this.datasets.putAll(datasets);
    }

    private void createAndImportLayer(DatasetId id, DatasetConfig config) {
        try {
            final Function<DatasetLocator, Source> func = sourceFactories.get(config.locator().getClass());
            if (func == null) {
                throw new IllegalArgumentException("Unrecognised config type " + config.locator().getClass());
            }

            final Source source = func.apply(config.locator());

            final LayerId layerId = LayerId.of(id.uid());
            final Subscriber<SourceUpdate> subscriber = layerStore.createLayer(
                    layerId,
                    datasetMetadataFrom(config.metadata()),
                    source.indexable(),
                    source.viewType().getLayerView());
            source.getObservable().subscribeOn(scheduler).subscribe(subscriber);   // TODO: the scheduler should be chosen by the specific source
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
