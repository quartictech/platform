package io.quartic.weyl.core.live.feed;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.time.ZonedDateTime;

@SweetStyle
@Value.Immutable
public interface AbstractFeedEvent {
    FeedIcon icon();
    FeedUser user();
    ZonedDateTime time();
    String body();
}
