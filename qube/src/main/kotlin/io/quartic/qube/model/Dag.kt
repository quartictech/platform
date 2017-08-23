package io.quartic.qube.model

data class DagNodeData(
    val id: String,
    val title: String,
    val type: String
)

data class DagEdgeData(
    val id: Long,
    val source: String,
    val target: String
)

data class DagNode(
    val data: DagNodeData
)

data class DagEdge(
    val data: DagEdgeData
)

data class Dag(
    val nodes: List<DagNode>,
    val edges: List<DagEdge>
)
