package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.Map;

@SweetStyle
@Value.Immutable
public interface Attributes {
    Attributes EMPTY_ATTRIBUTES = Collections::emptyMap;

    @JsonValue
    Map<AttributeName, Object> attributes();
}
