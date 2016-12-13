package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerStats;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

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
        LayerMetadata metadata();
        LayerStats stats();
        AttributeSchema schema();
        boolean live();
    }

    Map<LayerId, LayerInfo> layers();

    static LayerListUpdateMessage of(Collection<Layer> layers) {
        return LayerListUpdateMessageImpl.of(
                layers.stream().collect(toMap(
                        l -> l.spec().id(),
                        l -> LayerInfoImpl.of(
                                l.spec().metadata(),
                                l.stats(),
                                l.spec().schema(),
                                !l.spec().indexable()
                        )
                )));
    }
}
