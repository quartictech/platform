package io.quartic.eval.api.model

data class CytoscapeNodeData(
    val id: String,
    val title: String,
    val type: String
)

data class CytoscapeEdgeData(
    val id: Long,
    val source: String,
    val target: String
)

data class CytoscapeNode(
    val data: CytoscapeNodeData
)

data class CytoscapeEdge(
    val data: CytoscapeEdgeData
)

data class CytoscapeDag(
    val nodes: Set<CytoscapeNode>,
    val edges: Set<CytoscapeEdge>
)
