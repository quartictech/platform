package io.quartic.eval.api.model

data class CytoscapeDagNodeData(
    val id: String,
    val title: String,
    val type: String
)

data class CytoscapeDagEdgeData(
    val id: Long,
    val source: String,
    val target: String
)

data class CytoscapeDagNode(
    val data: CytoscapeDagNodeData
)

data class CytoscapeDagEdge(
    val data: CytoscapeDagEdgeData
)

data class CytoscapeDag(
    val nodes: Set<CytoscapeDagNode>,
    val edges: Set<CytoscapeDagEdge>
)
