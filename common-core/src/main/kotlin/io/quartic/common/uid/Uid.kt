package io.quartic.common.uid

import com.fasterxml.jackson.annotation.JsonValue

// TODO: can make this more Kotlinesque (e.g. an extension method for toString)

// Base class for all UID classes
abstract class Uid {
    @JsonValue
    abstract fun uid(): String   // TODO: should be a Long, but that breaks a lot of Javascript code due to type coercion for object keys

    override fun toString(): String {
        return uid()
    }
}
