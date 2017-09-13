package io.quartic.quarty.api.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Dataset(
    val namespace: String?,
    @JsonProperty("dataset_id")
    val datasetId: String
) {
    @get:JsonIgnore
    val fullyQualifiedName get() = "${namespace ?: ""}::${datasetId}"
}

data class Step(
    val id: String,
    val name: String,
    val description: String?,
    val file: String,
    @JsonProperty("line_range")
    val lineRange: List<Int>,
    val inputs: List<Dataset>,
    val outputs: List<Dataset>
)

data class Pipeline(
    val steps: List<Step>
)