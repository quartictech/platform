package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface GeofenceGeometryUpdateMessage extends SocketMessage {
    FeatureCollection featureCollection();
}
