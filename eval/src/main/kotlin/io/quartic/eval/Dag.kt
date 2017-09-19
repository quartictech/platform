package io.quartic.eval

import com.google.common.base.Preconditions.checkArgument
import io.quartic.common.logging.logger
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
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
        val LOG by logger()

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
}
