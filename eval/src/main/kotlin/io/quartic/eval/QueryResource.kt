package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.quarty.api.model.Pipeline.Node
import javax.ws.rs.NotFoundException

class QueryResource(private val database: Database) : EvalQueryService {
    override fun getBuilds(customerId: CustomerId) = database.getBuilds(customerId)
        .map { buildRow ->
            Build(
                id = buildRow.id,
                buildNumber = buildRow.buildNumber,
                branch = buildRow.branch,
                customerId = buildRow.customerId,
                trigger = buildRow.trigger.trigger,
                status = buildRow.status,
                time = buildRow.time
            )
        }

    override fun getDag(customerId: CustomerId): CytoscapeDag {
        val buildNumber = database.getLatestSuccessfulBuildNumber(customerId) ?:
            throw NotFoundException("No successful builds for ${customerId}")

        return getDag(customerId, buildNumber)
    }

    override fun getDag(customerId: CustomerId, buildNumber: Long): CytoscapeDag {
        // TODO - this linear scan is going to be expensive
        val output = database.getEventsForBuild(customerId, buildNumber)
            .mapNotNull { row ->
                ((row.payload as? PhaseCompleted)
                    ?.result as? Success)
                    ?.artifact as? EvaluationOutput
            }
            .firstOrNull() ?: throw NotFoundException("No DAG found for ${customerId} with build number ${buildNumber}")

        return convertToCytoscape(output.nodes)
    }

    // TODO - We're assuming that the DAG was validated before being added to the database
    private fun convertToCytoscape(nodes: List<Node>) = with(Dag.fromRaw(nodes)) {
        CytoscapeDag(nodesFrom(this), edgesFrom(this))
    }

    private fun nodesFrom(dag: Dag) = dag.nodes.map {
        CytoscapeNode(
            CytoscapeNodeData(
                id = it.output.fullyQualifiedName,
                title = it.output.fullyQualifiedName,
                type = if (it is Node.Raw) "raw" else "derived"
            )
        )
    }.toSet()

    private fun edgesFrom(dag: Dag) = dag.edges.mapIndexed { i, it ->
        CytoscapeEdge(
            CytoscapeEdgeData(
                id = i.toLong(),
                source = it.first.output.fullyQualifiedName,
                target = it.second.output.fullyQualifiedName
            )
        )
    }.toSet()
}
