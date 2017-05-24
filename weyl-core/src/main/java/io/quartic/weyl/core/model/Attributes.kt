package io.quartic.weyl.core.model

import com.fasterxml.jackson.annotation.JsonValue

interface Attributes {
    @get:JsonValue
    val attributes: Map<AttributeName, Any>

    companion object {
        val EMPTY_ATTRIBUTES = NaiveAttributes(emptyMap())

        data class NaiveAttributes(override val attributes: Map<AttributeName, Any>) : Attributes

        fun of(map: Map<AttributeName, Any>) = NaiveAttributes(map)
    }
}