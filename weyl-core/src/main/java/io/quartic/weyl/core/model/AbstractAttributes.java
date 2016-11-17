package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractAttributes {
    Attributes EMPTY_ATTRIBUTES = Attributes.builder().build();

    Map<AttributeName, Object> attributes();
}
