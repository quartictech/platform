package io.quartic.catalogue.api.model

import com.fasterxml.jackson.annotation.JsonValue

data class DatasetNamespace constructor(@get:JsonValue val namespace: String)