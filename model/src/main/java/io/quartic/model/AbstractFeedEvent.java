package io.quartic.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFeedEvent {
    String source();
    String message();

    @JsonAnyGetter
    Map<String, Object> properties();
}
