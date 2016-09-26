package io.quartic.weyl.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableGeofenceRequest.class)
public interface GeofenceRequest {
    GeofenceType type();
    AbstractFeatureCollection features();
}
