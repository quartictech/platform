package io.quartic.model;

import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
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
