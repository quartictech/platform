package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerStats;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerUpdateMessageImpl.class)
@JsonDeserialize(as = LayerUpdateMessageImpl.class)
public interface LayerUpdateMessage extends SocketMessage {
    LayerId layerId();
    DynamicSchema dynamicSchema();
    LayerStats stats();
    FeatureCollection featureCollection();
}
