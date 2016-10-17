package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractLayerIcon {
    @JsonValue
    String icon();
}
