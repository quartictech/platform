package io.quartic.weyl.core.live;

import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractEnrichedLiveEvent {
    LiveEventId eventId();
    LiveEvent liveEvent();
}
