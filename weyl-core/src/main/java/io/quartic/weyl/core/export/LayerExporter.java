package io.quartic.weyl.core.export;

import io.quartic.catalogue.api.*;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.MapDatasetExtensionImpl;
import io.quartic.weyl.core.source.ExtensionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Map;
import java.util.Optional;

import static io.quartic.common.rx.RxUtilsKt.*;

public class LayerExporter {
    private static final Logger LOG = LoggerFactory.getLogger(LayerExporter.class);
    private final Observable<Map<LayerId, LayerSnapshotSequence>> layers;
    private final LayerWriter layerWriter;
    private final CatalogueService catalogueService;

    public LayerExporter(Observable<LayerSnapshotSequence> snapshotSequences, LayerWriter layerWriter,
                         CatalogueService catalogueService) {
        this.layers = snapshotSequences.compose(accumulateMap(snapshot -> snapshot.spec().id(), snapshot -> snapshot))
                .compose(likeBehavior());
        this.layerWriter = layerWriter;
        this.catalogueService = catalogueService;
    }

    public Observable<LayerExportResult> export(LayerExportRequest layerExportRequest) {
        return Observable.combineLatest(
                layers, Observable.just(layerExportRequest), this::fetchLayerAndExport);
    }

    Optional<Layer> fetchLayer(Map<LayerId, LayerSnapshotSequence> layers, LayerExportRequest exportRequest) {
        return Optional.ofNullable(layers.get(exportRequest.layerId()))
                .map(snapshotSequence -> latest(snapshotSequence.snapshots()).absolute());
    }

    LayerExportResult fetchLayerAndExport(Map<LayerId, LayerSnapshotSequence> layers, LayerExportRequest exportRequest) {
        return fetchLayer(layers, exportRequest)
                .map(layer -> {
                    LayerExportResult exportResult = layerWriter.write(layer);
                    exportResult.locator().ifPresent( locator ->
                            catalogueService.registerDataset(datasetConfig(layer, locator)));
                    return exportResult;
                })
                .orElse(LayerExportResult.failure("couldn't find layer for export: " + exportRequest.layerId()));
    }

      DatasetConfig datasetConfig(Layer layer, DatasetLocator locator) {
        return DatasetConfigImpl.of(
                DatasetMetadataImpl.of(
                        layer.spec().metadata().name(),
                        layer.spec().metadata().description(),
                        layer.spec().metadata().attribution(),
                        Optional.empty(),
                        layer.spec().metadata().icon()
                ),
                locator,
                new ExtensionParser().unparse(layer.spec().metadata().name(),
                        MapDatasetExtensionImpl.of(LayerViewType.MOST_RECENT, layer.spec().staticSchema()))
        );
    }


}
