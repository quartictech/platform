package io.quartic.weyl.core.model

import com.fasterxml.jackson.annotation.JsonValue

data class AttributeName(@get:JsonValue val name: String) {
    override fun toString() = name
}