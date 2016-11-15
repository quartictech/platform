package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractAttributeName {
    @JsonUnwrapped
    String name();
}
