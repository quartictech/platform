package io.quartic.catalogue.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface Icon {
    @JsonValue
    String icon();
}
