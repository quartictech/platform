package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AttributeName {
    @JsonValue
    String name();
}
