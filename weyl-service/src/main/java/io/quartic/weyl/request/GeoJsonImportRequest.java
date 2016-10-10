package io.quartic.weyl.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quartic.weyl.core.geojson.FeatureCollection;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as=ImmutableGeoJsonImportRequest.class)
public interface GeoJsonImportRequest {
    String name();
    String description();
    FeatureCollection data();
}
