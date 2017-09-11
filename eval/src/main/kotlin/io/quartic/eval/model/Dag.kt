package io.quartic.eval.model

import com.google.common.base.Preconditions.checkArgument
import io.quartic.eval.model.Dag.Node
import io.quartic.quarty.api.model.Dataset
import io.quartic.quarty.api.model.Step
import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.traverse.TopologicalOrderIterator

class Dag(private val dag: DirectedGraph<Node, DummyEdge>) : Iterable<Node> {
    data class Node(
        val dataset: Dataset,
        val step: Step? = null  // TODO - eventually we probably want steps representing dataset creation, i.e. non-nullable
    )

    class DummyEdge

    val nodes: Set<Node> = dag.vertexSet()
    val edges by lazy { nodes.flatMap { source -> dag.outgoingEdgesOf(source).map { source to dag.getEdgeTarget(it) } } }
    fun inDegreeOf(node: Node) = dag.inDegreeOf(node)
    fun outDegreeOf(node: Node) = dag.outDegreeOf(node)

    override fun iterator(): Iterator<Node> = TopologicalOrderIterator(dag)

    companion object {
        fun fromSteps(steps: List<Step>): Dag {
            val dag = DefaultDirectedGraph<Node, DummyEdge>(DummyEdge::class.java)

            val allDatasets = mutableMapOf<Dataset, Node>()
            steps.forEach { step ->
                checkArgument(step.outputs.size == 1, "${step} doesn't produce exactly one output")

                val output = step.outputs[0]
                checkArgument(
                    output !in allDatasets || allDatasets[output]!!.step == null,
                    "${output} produced by multiple datasets"
                )

                val node = Node(output, step)
                allDatasets[output] = (node)

                step.inputs.forEach { input ->
                    if (input !in allDatasets) {
                        val raw = Node(input)
                        allDatasets[input] = raw
                    }
                }
            }

            allDatasets.forEach { _, node -> dag.addVertex(node) }

            allDatasets.forEach { _, node ->
                node.step?.inputs?.forEach { input ->
                    dag.addEdge(allDatasets[input], node)
                }
            }

            checkArgument(!CycleDetector(dag).detectCycles(), "Graph contains cycles")

            return Dag(dag)
        }

    }
}
