package io.quartic.weyl.core.render;

import io.quartic.common.geojson.GeoJsonGenerator;
import io.quartic.common.rx.RxUtilsKt;
import io.quartic.howl.api.HowlClient;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.Map;

import static io.quartic.common.rx.RxUtilsKt.accumulateMap;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;

public class LayerExporter {
    private static final Logger LOG = LoggerFactory.getLogger(LayerExporter.class);
    private final Observable<Map<LayerId, LayerSnapshotSequence>> layers;
    private final HowlClient howlClient;
    private final FeatureConverter featureConverter;

    public LayerExporter(Observable<LayerSnapshotSequence> snapshotSequences, HowlClient howlClient,
                         FeatureConverter featureConverter) {
        this.layers = snapshotSequences.compose(accumulateMap(snapshot -> snapshot.spec().id(), snapshot -> snapshot))
                .compose(likeBehavior());
       this.howlClient = howlClient;
       this.featureConverter = featureConverter;
    }

    public Observable<LayerExportResult> export(LayerExportRequest layerExportRequest) {
        return Observable.combineLatest(
                layers, Observable.just(layerExportRequest), this::fetchLayerAndExport);
    }

    private LayerExportResult fetchLayerAndExport(Map<LayerId, LayerSnapshotSequence> layers, LayerExportRequest exportRequest) {
        if (! layers.containsKey(exportRequest.layerId())) {
            LOG.info("seeing layers: {}", layers);
            return LayerExportResult.failure("couldn't find layer to export with id " + exportRequest.layerId());
        }
        Layer layer = RxUtilsKt.latest(layers.get(exportRequest.layerId())
                .snapshots())
                .absolute();

        try {
            return exportLayer(layer);
        } catch (IOException e) {
            e.printStackTrace();
            return LayerExportResult.failure("error while writing to storage");
        }
    }

    private LayerExportResult exportLayer(Layer layer) throws IOException {
        final int[] featureCount = new int[1];
        HowlStorageId howlStorageId = howlClient.uploadFile("application/json", "weyl", outputStream -> {
            GeoJsonGenerator geoJsonGenerator = new GeoJsonGenerator(outputStream);

            featureCount[0] = geoJsonGenerator.writeFeatures(layer.features().stream()
                .map(featureConverter::featureToGeojson));
        });
        return LayerExportResult.success(howlStorageId, featureCount[0]);
    }
}
