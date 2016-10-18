package io.quartic.weyl.message;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractGeofenceUpdateMessage extends SocketMessage {
    FeatureCollection featureCollection();
}
