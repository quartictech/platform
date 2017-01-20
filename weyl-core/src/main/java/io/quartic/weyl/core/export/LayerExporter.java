package io.quartic.weyl.core.export;

import io.quartic.catalogue.api.*;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.MapDatasetExtensionImpl;
import io.quartic.weyl.core.source.ExtensionCodec;
import rx.Observable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static io.quartic.common.rx.RxUtilsKt.*;

public class LayerExporter {
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

    public Observable<Optional<LayerExportResult>> export(LayerExportRequest layerExportRequest) {
        return Observable.combineLatest(
                layers, Observable.just(layerExportRequest), this::fetchLayerAndExport);
    }

    private Optional<Layer> fetchLayer(Map<LayerId, LayerSnapshotSequence> layers, LayerExportRequest exportRequest) {
        return Optional.ofNullable(layers.get(exportRequest.layerId()))
                .map(snapshotSequence -> latest(snapshotSequence.snapshots()).absolute());
    }

    private Optional<LayerExportResult> fetchLayerAndExport(Map<LayerId, LayerSnapshotSequence> layers,
                                                            LayerExportRequest exportRequest) {
        return fetchLayer(layers, exportRequest)
                .map(layer -> {
                    try {
                        LayerExportResult exportResult = layerWriter.write(layer);
                        catalogueService.registerDataset(datasetConfig(layer, exportResult.locator()));
                        return exportResult;
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                });
    }

    private DatasetConfig datasetConfig(Layer layer, DatasetLocator locator) {
        return DatasetConfigImpl.of(
                DatasetMetadataImpl.of(
                        String.format("%s (exported %s)", layer.spec().metadata().name(),
                                DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())),
                        layer.spec().metadata().description(),
                        layer.spec().metadata().attribution(),
                        Optional.empty(),
                        layer.spec().metadata().icon()
                ),
                locator,
                new ExtensionCodec().encode(MapDatasetExtensionImpl.of(LayerViewType.MOST_RECENT, layer.spec().staticSchema()))
        );
    }
}
