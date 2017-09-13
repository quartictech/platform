package io.quartic.eval.model

import com.google.common.base.Preconditions.checkArgument
import io.quartic.quarty.api.model.Pipeline.Dataset
import io.quartic.quarty.api.model.Pipeline.Node
import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.traverse.TopologicalOrderIterator

class Dag(private val dag: DirectedGraph<Node, DummyEdge>) : Iterable<Node> {
    class DummyEdge

    val nodes: Set<Node> = dag.vertexSet()
    val edges: List<Pair<Node, Node>> by lazy {
        nodes.flatMap { source ->
            dag.outgoingEdgesOf(source).map { source to dag.getEdgeTarget(it) }
        }
    }
    fun inDegreeOf(node: Node) = dag.inDegreeOf(node)
    fun outDegreeOf(node: Node) = dag.outDegreeOf(node)

    override fun iterator(): Iterator<Node> = TopologicalOrderIterator(dag)

    companion object {
        fun fromRaw(nodes: List<Node>): Dag {
            val dag = DefaultDirectedGraph<Node, DummyEdge>(DummyEdge::class.java)

            val datasetsToNodes = mutableMapOf<Dataset, Node>()

            nodes.forEach { node ->
                checkArgument(node.output !in datasetsToNodes,
                    "Dataset ${node.output.fullyQualifiedName} produced by multiple nodes")
                datasetsToNodes[node.output] = node
                dag.addVertex(node)
            }

            nodes.forEach { node ->
                node.inputs.forEach { input ->
                    dag.addEdge(datasetsToNodes[input], node)
                }
            }

            checkArgument(!CycleDetector(dag).detectCycles(), "Graph contains cycles")

            return Dag(dag)
        }

    }
}
