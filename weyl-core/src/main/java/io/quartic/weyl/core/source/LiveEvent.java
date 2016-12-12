package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import org.immutables.value.Value;

import java.time.Instant;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LiveEventImpl.class)
@JsonDeserialize(as = LiveEventImpl.class)
public interface LiveEvent {
    Instant timestamp();
    FeatureCollection featureCollection();
}
