package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonValue;

// Base class for all UID classes
public interface Uid {
    @JsonValue
    String uid();  // TODO: should be a Long, but that breaks a lot of Javascript code due to type coercion for object keys
}
