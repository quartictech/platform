package io.quartic.quarty.api.model

data class Dataset(
    val namespace: String?,
    val datasetId: String
)

data class Step(
    val id: String,
    val name: String,
    val description: String?,
    val file: String,
    val lineRange: List<Int>,
    val inputs: List<Dataset>,
    val outputs: List<Dataset>
)

data class Pipeline(
    val steps: List<Step>
)
