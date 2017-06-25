package io.quartic.zeus.model

import com.fasterxml.jackson.annotation.JsonValue

data class ItemId(@get:JsonValue val id: String) { override fun toString() = id }