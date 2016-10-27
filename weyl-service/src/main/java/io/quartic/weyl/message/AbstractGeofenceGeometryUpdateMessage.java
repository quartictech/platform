package io.quartic.weyl.message;

import io.quartic.weyl.common.SweetStyle;
import io.quartic.weyl.core.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractGeofenceGeometryUpdateMessage extends SocketMessage {
    FeatureCollection featureCollection();
}
