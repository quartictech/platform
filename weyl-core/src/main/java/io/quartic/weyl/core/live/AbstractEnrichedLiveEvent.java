package io.quartic.weyl.core.live;

import io.quartic.model.LiveEvent;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractEnrichedLiveEvent {
    LiveEventId eventId();
    LiveEvent liveEvent();
}
