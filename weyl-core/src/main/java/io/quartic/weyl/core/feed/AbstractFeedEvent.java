package io.quartic.weyl.core.feed;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.time.ZonedDateTime;

@SweetStyle
@Value.Immutable
public interface AbstractFeedEvent {
    FeedUser user();
    ZonedDateTime time();
    String body();
}
