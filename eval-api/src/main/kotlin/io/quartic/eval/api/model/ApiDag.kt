package io.quartic.eval.api.model

data class ApiDag(val nodes: List<Node>) {
    data class Node(
        val namespace: String?,
        val datasetId: String,
        val sources: List<Int>
    )
}
