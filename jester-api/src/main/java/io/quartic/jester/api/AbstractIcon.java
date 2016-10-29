package io.quartic.jester.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractIcon {
    @JsonValue
    String icon();
}
