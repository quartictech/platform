package io.quartic.eval.model

import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.Step
import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph

class Dag(private val dag: DirectedGraph<Dataset, PseudoEdge>) {
    data class PseudoEdge(
        val step: Step,
        val source: Dataset,
        val target: Dataset
    )

    val nodes: Set<Dataset> = dag.vertexSet()
    val edges: Set<PseudoEdge> = dag.edgeSet()

    fun inDegreeOf(node: Dataset) = dag.inDegreeOf(node)
    fun outDegreeOf(node: Dataset) = dag.outDegreeOf(node)

    fun validate() = checkNoCycles() && checkOneStepPerDataset()

    private fun checkNoCycles() = !CycleDetector(dag).detectCycles()
    private fun checkOneStepPerDataset() = dag.vertexSet().all { vertex ->
        dag.incomingEdgesOf(vertex).map { (step) -> step }.toSet().size <= 1
    }

    companion object {
        fun fromSteps(steps: List<Step>): Dag {
           val dag = DefaultDirectedGraph<Dataset, PseudoEdge>(PseudoEdge::class.java)

            steps.forEach { step ->
                step.inputs.forEach { input -> dag.addVertex(input) }
                step.outputs.forEach { output -> dag.addVertex(output) }

                step.inputs.forEach { input ->
                    step.outputs.forEach { output ->
                        dag.addEdge(input, output, PseudoEdge(step, input, output))
                    }
                }
            }

            return Dag(dag)
        }
    }
}
