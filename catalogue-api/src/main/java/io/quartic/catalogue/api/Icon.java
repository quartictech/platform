package io.quartic.catalogue.api;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = IconImpl.class)
@JsonDeserialize(as = IconImpl.class)
public interface Icon {
    @JsonValue
    String icon();
}
