package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.StaticSchema;
import org.immutables.value.Value;

import java.util.Set;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerListUpdateMessageImpl.class)
@JsonDeserialize(as = LayerListUpdateMessageImpl.class)
public interface LayerListUpdateMessage extends SocketMessage {

    @SweetStyle
    @Value.Immutable
    @JsonSerialize(as=LayerInfoImpl.class)
    @JsonDeserialize(as=LayerInfoImpl.class)
    interface LayerInfo {
        LayerId id();
        LayerMetadata metadata();
        StaticSchema staticSchema();
        boolean live();
    }

    Set<LayerInfo> layers();
}
