package io.quartic.zeus.model

import com.fasterxml.jackson.annotation.JsonValue

data class DatasetName(@get:JsonValue val name: String) { override fun toString() = name }