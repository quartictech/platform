package io.quartic.eval

import com.google.common.base.Preconditions.checkArgument
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.PhaseCompletedV7.Node
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

    override fun iterator(): Iterator<Node> = TopologicalOrderIterator(dag)

    companion object {
        private fun wrapValidationExceptions(nodes: List<Node>, block: (List<Node>) -> Dag): DagResult =
            try {
                DagResult.Valid(block(nodes))
            } catch (e: IllegalArgumentException) {
                DagResult.Invalid(e.message!!, nodes)
            }

        fun fromRawValidating(nodes: List<Node>) = wrapValidationExceptions(nodes) {
            fromRaw(nodes)
        }

        fun fromRaw(nodes: List<Node>): Dag {
            val dag = DefaultDirectedGraph<Node, DummyEdge>(DummyEdge::class.java)

            val datasetsToNodes = mutableMapOf<Dataset, Node>()

            nodes.forEach { node ->
                checkArgument(node.output !in datasetsToNodes,
                    "Dataset ${node.output.fullyQualifiedName} produced by multiple nodes")
                datasetsToNodes[node.output] = node
                dag.addVertex(node)
            }

            val unproducedDatasets = mutableSetOf<Dataset>()
            nodes.forEach { node ->
                node.inputs.forEach { input ->
                    if (input !in datasetsToNodes) {
                        unproducedDatasets.add(input)
                    } else {
                        dag.addEdge(datasetsToNodes[input], node)
                    }
                }
            }
            checkArgument(unproducedDatasets.isEmpty(),
                "Datasets are not produced by any node: ${unproducedDatasets.joinToString("\n")}")

            checkArgument(!CycleDetector(dag).detectCycles(), "Graph contains cycles")

            return Dag(dag)
        }

    }

    sealed class DagResult {
        data class Valid(val dag: Dag): DagResult()
        data class Invalid(val error: String, val nodes: List<Node>): DagResult()
    }
}
