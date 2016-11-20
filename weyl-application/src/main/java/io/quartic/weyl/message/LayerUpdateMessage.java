package io.quartic.weyl.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerUpdateMessageImpl.class)
@JsonDeserialize(as = LayerUpdateMessageImpl.class)
public interface LayerUpdateMessage extends SocketMessage {
    LayerId layerId();
    AttributeSchema schema();
    FeatureCollection featureCollection();
}
