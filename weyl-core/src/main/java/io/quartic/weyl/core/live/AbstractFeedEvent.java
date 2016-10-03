package io.quartic.weyl.core.live;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFeedEvent {
    String source();
    String message();

    @JsonAnyGetter
    Map<String, String> properties();
}
