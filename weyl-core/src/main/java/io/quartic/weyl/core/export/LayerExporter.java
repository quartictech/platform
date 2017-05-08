package io.quartic.weyl.core.export;

import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.catalogue.api.model.DatasetNamespace;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.source.ExtensionCodec;
import rx.Observable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static io.quartic.common.rx.RxUtilsKt.accumulateMap;
import static io.quartic.common.rx.RxUtilsKt.latest;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;
import static io.quartic.weyl.core.live.LayerViewType.MOST_RECENT;

public class LayerExporter {
    private final Observable<Map<LayerId, LayerSnapshotSequence>> layers;
    private final LayerWriter layerWriter;
    private final CatalogueService catalogue;
    private final DatasetNamespace defaultCatalogueNamespace;

    public LayerExporter(
            Observable<LayerSnapshotSequence> snapshotSequences,
            LayerWriter layerWriter,
            CatalogueService catalogue,
            DatasetNamespace defaultCatalogueNamespace
    ) {
        this.layers = snapshotSequences
                .compose(accumulateMap(snapshot -> snapshot.getSpec().getId(), snapshot -> snapshot))
                .compose(likeBehavior());
        this.layerWriter = layerWriter;
        this.catalogue = catalogue;
        this.defaultCatalogueNamespace = defaultCatalogueNamespace;
    }

    public Observable<Optional<LayerExportResult>> export(LayerExportRequest layerExportRequest) {
        return Observable.combineLatest(
                layers, Observable.just(layerExportRequest), this::fetchLayerAndExport);
    }

    private Optional<Layer> fetchLayer(Map<LayerId, LayerSnapshotSequence> layers, LayerExportRequest exportRequest) {
        return Optional.ofNullable(layers.get(exportRequest.getLayerId()))
                .map(snapshotSequence -> latest(snapshotSequence.getSnapshots()).getAbsolute());
    }

    private Optional<LayerExportResult> fetchLayerAndExport(Map<LayerId, LayerSnapshotSequence> layers,
                                                            LayerExportRequest exportRequest) {
        return fetchLayer(layers, exportRequest)
                .map(layer -> {
                    try {
                        LayerExportResult exportResult = layerWriter.write(layer);
                        catalogue.registerDataset(defaultCatalogueNamespace, datasetConfig(layer, exportResult.getLocator()));
                        return exportResult;
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                });
    }

    private DatasetConfig datasetConfig(Layer layer, DatasetLocator locator) {
        return new DatasetConfig(
                new DatasetMetadata(
                        String.format("%s (exported %s)", layer.getSpec().getMetadata().getName(),
                                DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())),
                        layer.getSpec().getMetadata().getDescription(),
                        layer.getSpec().getMetadata().getAttribution(),
                        null
                ),
                locator,
                new ExtensionCodec().encode(new MapDatasetExtension(layer.getSpec().getStaticSchema(), MOST_RECENT))
        );
    }
}
