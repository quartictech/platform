package io.quartic.weyl.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as=ImmutableGeoJsonImportRequest.class)
public interface GeoJsonImportRequest {
    @JsonUnwrapped
    LayerMetadata metadata();
    FeatureCollection data();
}
