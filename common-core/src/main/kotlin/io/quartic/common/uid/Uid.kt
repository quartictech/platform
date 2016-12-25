package io.quartic.common.uid

import com.fasterxml.jackson.annotation.JsonValue

/** Base class for all UID classes */
abstract class Uid(val uid: String) { // TODO: should be a Long, but that breaks a lot of Javascript code due to type coercion for object keys
    @JsonValue
    override fun toString() = uid

    // If this were a data class, then would be no need for below.  Alas data classes are final, so can't get distinct subtypes

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return (uid == (other as Uid).uid)
    }

    override fun hashCode() = uid.hashCode()
}
