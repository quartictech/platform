package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.model.AbstractAttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateMessage extends SocketMessage {
    LayerId layerId();
    AbstractAttributeSchema schema();
    FeatureCollection featureCollection();
}
