package io.quartic.weyl.core.feed;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.time.ZonedDateTime;

@SweetStyle
@Value.Immutable
public interface AbstractElaboratedFeedEvent {
    LayerId layerId();
    String featureId();
    FeedIcon icon();
    FeedUser user();
    ZonedDateTime time();
    String body();
}
