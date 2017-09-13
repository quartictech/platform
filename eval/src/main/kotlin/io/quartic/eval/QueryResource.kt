package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.*
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.PhaseCompleted
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.quarty.api.model.Step
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
            .map { println(it); it.toApi() }

    private fun Database.EventRow.toApi() = when (this.payload) {
        is BuildEvent.LogMessageReceived -> ApiBuildEvent.Log(
            message = this.payload.message,
            phaseId = this.payload.phaseId,
            time = this.time,
            stream = this.payload.stream
        )
        else -> ApiBuildEvent.Other(this.time)
    }

    override fun getBuilds(customerId: CustomerId) = database.getBuilds(customerId, null)
        .map { it.toBuild() }

    private fun Database.BuildStatusRow.toBuild() = Build(
        id = this.id,
        buildNumber = this.buildNumber,
        branch = this.branch,
        customerId = this.customerId,
        trigger = this.trigger.trigger,
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

        return convertToCytoscape(output.steps)
    }

    // TODO - We're assuming that the DAG was validated before being added to the database
    private fun convertToCytoscape(steps: List<Step>) = with(Dag.fromSteps(steps)) {
        CytoscapeDag(nodesFrom(this), edgesFrom(this))
    }

    private fun nodesFrom(dag: Dag) = dag.nodes.map {
        CytoscapeNode(
            CytoscapeNodeData(
                id = it.dataset.fullyQualifiedName,
                title = it.dataset.fullyQualifiedName,
                type = if (dag.inDegreeOf(it) > 0) "derived" else "raw"
            )
        )
    }.toSet()

    private fun edgesFrom(dag: Dag) = dag.edges.mapIndexed { i, it ->
        CytoscapeEdge(
            CytoscapeEdgeData(
                id = i.toLong(),
                source = it.first.dataset.fullyQualifiedName,
                target = it.second.dataset.fullyQualifiedName
            )
        )
    }.toSet()
}
