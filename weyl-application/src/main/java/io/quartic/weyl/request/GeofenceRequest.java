package io.quartic.weyl.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(as = ImmutableGeofenceRequest.class)
public interface GeofenceRequest {
    GeofenceType type();
    Optional<FeatureCollection> features();
    Optional<LayerId> layerId();
    double bufferDistance();    // TODO: what units?
}
