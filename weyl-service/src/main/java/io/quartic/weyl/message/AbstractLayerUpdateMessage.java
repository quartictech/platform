package io.quartic.weyl.message;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.live.LiveLayerState;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateMessage extends SocketMessage {
    LayerId layerId();
    @JsonUnwrapped
    LiveLayerState state();
}
