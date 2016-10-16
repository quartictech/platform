package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AbstractLiveEvent {
    Instant timestamp();
    Optional<FeatureCollection> featureCollection();
    Optional<FeedEvent> feedEvent();
}
