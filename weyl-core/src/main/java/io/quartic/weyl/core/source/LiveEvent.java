package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.model.LayerUpdate;
import org.immutables.value.Value;

import java.time.Instant;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LiveEventImpl.class)
@JsonDeserialize(as = LiveEventImpl.class)
public interface LiveEvent {
    @Value.Default
    default LayerUpdate.Type updateType() {
        // for back-compat
        return LayerUpdate.Type.APPEND;
    }

    Instant timestamp();
    FeatureCollection featureCollection();
}
