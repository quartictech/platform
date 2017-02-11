package io.quartic.weyl.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.geojson.FeatureCollection;
import org.immutables.value.Value;

import java.time.Instant;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LiveEventImpl.class)
@JsonDeserialize(as = LiveEventImpl.class)
public interface LiveEvent {
    @Value.Default
    default LayerUpdateType updateType() {
        // for back-compat
        return LayerUpdateType.APPEND;
    }
    Instant timestamp();
    FeatureCollection featureCollection();
}
