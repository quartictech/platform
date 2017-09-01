package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.Database.ValidDagRow
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.quarty.model.Dataset
import javax.ws.rs.NotFoundException

class QueryResource(private val database: Database) : EvalQueryService {
    override fun getDag(customerId: CustomerId, buildNumber: Long) = convertToCytoscape(
        database.getValidDag(customerId, buildNumber) ?:
            throw NotFoundException("No DAG registered for ${customerId} with build number ${buildNumber}")
    )

    override fun getDag(customerId: CustomerId) = convertToCytoscape(
        database.getLatestValidDag(customerId) ?: throw NotFoundException("No DAG registered for ${customerId}")
    )

    private fun foo(customerId: CustomerId, buildNumber: Long) {
        val events = database.getEventsForBuild(customerId, buildNumber)

        val output = events.mapNotNull { row ->
            ((row.payload as? PhaseCompleted)
                ?.result as? Success)
                ?.artifact as? EvaluationOutput
        }.firstOrNull()
    }

    // TODO - We're assuming that the DAG was validated before being added to the database
    private fun convertToCytoscape(success: ValidDagRow) = with(Dag.fromSteps(success.artifact.steps)) {
        CytoscapeDag(nodesFrom(this), edgesFrom(this))
    }

    private fun nodesFrom(dag: Dag) = dag.nodes.map {
        CytoscapeNode(
                CytoscapeNodeData(
                    id = it.title,
                    title = it.title,
                    type = if (dag.inDegreeOf(it) > 0) "derived" else "raw"
                )
        )
    }.toSet()

    private fun edgesFrom(dag: Dag) = dag.edges.mapIndexed { i, it ->
        CytoscapeEdge(
            CytoscapeEdgeData(
                id = i.toLong(),
                source = it.source.title,
                target = it.target.title
            )
        )
    }.toSet()

    private val Dataset.title get() = "${namespace ?: ""}::${datasetId}"
}
