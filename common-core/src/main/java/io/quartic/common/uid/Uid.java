package io.quartic.common.uid;

import com.fasterxml.jackson.annotation.JsonValue;

// Base class for all UID classes
public abstract class Uid {
    @JsonValue
    public abstract String uid();  // TODO: should be a Long, but that breaks a lot of Javascript code due to type coercion for object keys

    @Override
    public String toString() {
        return uid();
    }
}
