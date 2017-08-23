package io.quartic.eval.model

import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph

class DAG (val dag: DirectedGraph<Dataset, PseudoEdge>) {
    data class PseudoEdge(
        val step: Step,
        val source: Dataset,
        val target: Dataset
    )

    fun checkNoCycles() = !CycleDetector(dag).detectCycles()
    fun checkOneStepPerDataset() = dag.vertexSet().all { vertex ->
        dag.incomingEdgesOf(vertex).map { (step) -> step }.toSet().size <= 1
    }

    fun validate() = checkNoCycles() && checkOneStepPerDataset()

    companion object {
        fun fromSteps(steps: List<Step>): DAG {
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

            return DAG(dag)
        }
    }
}
