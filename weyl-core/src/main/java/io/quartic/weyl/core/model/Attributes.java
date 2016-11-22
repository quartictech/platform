package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.Map;

// Not Immutable - we need people to use AttributesFactory
public interface Attributes {
    Attributes EMPTY_ATTRIBUTES = Collections::emptyMap;

    @JsonValue
    Map<AttributeName, Object> attributes();
}
