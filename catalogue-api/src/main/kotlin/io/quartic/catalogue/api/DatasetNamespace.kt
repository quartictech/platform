package io.quartic.catalogue.api

import com.fasterxml.jackson.annotation.JsonValue

data class DatasetNamespace constructor(@get:JsonValue val namespace: String)