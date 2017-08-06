package io.quartic.bild.api


data class NodeData(
    val id: String,
    val title: String,
    val type: String
)

data class Node(
    val data: NodeData
)

data class EdgeData(
    val id: String,
    val source: String,
    val target: String
)

data class Edge(
    val data: EdgeData
)

data class Dag(
   val nodes: List<Node>,
   val edges: List<Edge>
)
