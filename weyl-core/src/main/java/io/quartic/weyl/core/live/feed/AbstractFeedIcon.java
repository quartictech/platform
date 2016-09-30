package io.quartic.weyl.core.live.feed;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractFeedIcon {
    @JsonValue
    String name();
}
