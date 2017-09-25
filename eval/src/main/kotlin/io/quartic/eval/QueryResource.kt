package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.*
import io.quartic.eval.database.Database
import io.quartic.eval.database.model.PhaseCompletedV5.Result.Success
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Artifact.EvaluationOutput
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node
import io.quartic.eval.database.model.LogMessageReceived
import io.quartic.eval.database.model.PhaseCompleted
import io.quartic.eval.database.model.PhaseStarted
import io.quartic.eval.database.model.toApiModel
import javax.ws.rs.NotFoundException

class QueryResource(private val database: Database) : EvalQueryService {
    override fun getBuild(customerId: CustomerId, buildNumber: Long): Build {
        val builds = database.getBuilds(customerId, buildNumber)

        if (builds.isEmpty()) {
            throw NotFoundException(
                "Build not found: customerId = ${customerId}, buildNumber=${buildNumber}"
            )
        } else return builds.first().toBuild()
    }

    override fun getBuildEvents(customerId: CustomerId, buildNumber: Long): List<ApiBuildEvent> =
        database.getEventsForBuild(customerId, buildNumber)
            .map { it.toApi() }

    private fun Database.EventRow.toApi() = when (this.payload) {
        is LogMessageReceived -> ApiBuildEvent.Log(
            message = this.payload.message,
            phaseId = this.payload.phaseId,
            time = this.time,
            stream = this.payload.stream,
            id = this.id
        )
        is PhaseStarted -> ApiBuildEvent.PhaseStarted(
            phaseId = this.payload.phaseId,
            description = this.payload.description,
            time = this.time,
            id = this.id
        )
        is PhaseCompleted -> ApiBuildEvent.PhaseCompleted(
            this.payload.phaseId,
            this.time,
            this.id
        )
        else -> ApiBuildEvent.Other(this.time, this.id)
    }

    override fun getBuilds(customerId: CustomerId) = database.getBuilds(customerId, null)
        .map { it.toBuild() }

    private fun Database.BuildStatusRow.toBuild() = Build(
        id = this.id,
        buildNumber = this.buildNumber,
        branch = this.branch,
        customerId = this.customerId,
        trigger = this.trigger.trigger.toApiModel(),
        status = this.status,
        time = this.time
    )

    override fun getLatestDag(customerId: CustomerId): CytoscapeDag {
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
