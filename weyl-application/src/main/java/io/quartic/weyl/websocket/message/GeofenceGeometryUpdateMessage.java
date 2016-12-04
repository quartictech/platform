package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = GeofenceGeometryUpdateMessageImpl.class)
@JsonDeserialize(as = GeofenceGeometryUpdateMessageImpl.class)
public interface GeofenceGeometryUpdateMessage extends SocketMessage {
    FeatureCollection featureCollection();
}
