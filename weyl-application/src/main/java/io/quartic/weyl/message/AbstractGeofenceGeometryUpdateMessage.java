package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractGeofenceGeometryUpdateMessage extends SocketMessage {
    FeatureCollection featureCollection();
}
