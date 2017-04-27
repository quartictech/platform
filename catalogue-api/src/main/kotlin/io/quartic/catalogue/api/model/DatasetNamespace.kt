package io.quartic.catalogue.api.model

import com.fasterxml.jackson.annotation.JsonValue

data class DatasetNamespace(@get:JsonValue val namespace: String) {
    override fun toString() = namespace

    companion object {
        val NAMESPACE_ANY = DatasetNamespace("*")
    }
}