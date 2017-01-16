package io.quartic.weyl.core.export;

import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.common.geojson.GeoJsonGenerator;
import io.quartic.howl.api.HowlClient;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.Layer;

import java.io.IOException;

public class HowlGeoJsonLayerWriter implements LayerWriter {
    private static final String HOWL_NAMESPACE = "weyl";
    private final HowlClient howlClient;
    private final FeatureConverter featureConverter;

    public HowlGeoJsonLayerWriter(HowlClient howlClient, FeatureConverter featureConverter) {
        this.howlClient = howlClient;
        this.featureConverter = featureConverter;
    }


    public LayerExportResult storeToHowl(Layer layer) throws IOException {
        final int[] featureCount = new int[1];
        HowlStorageId howlStorageId = howlClient.uploadFile("application/json", "weyl", outputStream -> {
            GeoJsonGenerator geoJsonGenerator = new GeoJsonGenerator(outputStream);

            featureCount[0] = geoJsonGenerator.writeFeatures(layer.features().stream()
                .map(featureConverter::featureToGeojson));
        });
        return LayerExportResult.success(
                CloudGeoJsonDatasetLocatorImpl.of(String.format("/%s/%s", HOWL_NAMESPACE, howlStorageId)),
                String.format("wrote %d features to cloud storage", featureCount[0]));
    }

    @Override
    public LayerExportResult write(Layer layer) {
        try {
            return storeToHowl(layer);
        } catch (IOException e) {
            e.printStackTrace();
            return LayerExportResult.failure("exception while writing to cloud storage");
        }
    }
}
