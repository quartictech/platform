package io.quartic.weyl.core.live;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.time.Instant;

@SweetStyle
@Value.Immutable
public interface AbstractEnrichedFeedEvent {
    LiveEventId id();
    Instant timestamp();

    @JsonUnwrapped
    FeedEvent feedEvent();
}
