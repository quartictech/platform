package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.ApiDag
import io.quartic.eval.api.model.ApiPhaseCompletedResult
import io.quartic.eval.api.model.Build
import io.quartic.eval.database.Database
import io.quartic.eval.database.model.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node
import io.quartic.eval.database.model.PhaseCompletedV6.Artifact.EvaluationOutput
import io.quartic.eval.database.model.PhaseCompletedV6.Result.Success
import java.util.*
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

    override fun getBuildById(buildId: UUID): Build {
        return database.getBuild(buildId).toBuild()
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
            when (this.payload.result) {
                is PhaseCompletedV6.Result.Success ->
                    ApiPhaseCompletedResult.Success()
                is PhaseCompletedV6.Result.UserError ->
                    ApiPhaseCompletedResult.UserError(this.payload.result.info.toApi())
                is PhaseCompletedV6.Result.InternalError -> ApiPhaseCompletedResult.InternalError()
            },
            this.time,
            this.id
        )
        is TriggerReceived -> ApiBuildEvent.TriggerReceived(
            when (this.payload.trigger) {
                is CurrentTriggerReceived.BuildTrigger.Manual -> "manual"
                is CurrentTriggerReceived.BuildTrigger.GithubWebhook -> "github"
            },
            this.time,
            this.id
        )
        is BuildFailed -> ApiBuildEvent.BuildFailed(
            this.payload.description,
            this.time,
            this.id
        )
        else -> ApiBuildEvent.Other(this.time, this.id)
    }

    private fun LegacyPhaseCompleted.V5.UserErrorInfo.toApi() =  when(this) {
        is LegacyPhaseCompleted.V5.UserErrorInfo.InvalidDag ->
            this.error
        is LegacyPhaseCompleted.V5.UserErrorInfo.OtherException ->
            this.detail.toString()
    }

    override fun getBuilds(customerId: CustomerId) = database.getBuilds(customerId, null)
        .map { it.toBuild() }

    private fun Database.BuildRow.toBuild() = Build(
        id = this.id,
        buildNumber = this.buildNumber,
        branch = this.branch,
        customerId = this.customerId,
        trigger = this.trigger?.trigger?.toApiModel(),
        status = this.status,
        time = this.time
    )

    override fun getLatestDag(customerId: CustomerId): ApiDag {
        val buildNumber = database.getLatestSuccessfulBuildNumber(customerId) ?:
            throw NotFoundException("No successful builds for ${customerId}")

        return getDag(customerId, buildNumber)
    }

    override fun getDag(customerId: CustomerId, buildNumber: Long): ApiDag {
        // TODO - this linear scan is going to be expensive
        val output = database.getEventsForBuild(customerId, buildNumber)
            .mapNotNull { row ->
                ((row.payload as? PhaseCompleted)
                    ?.result as? Success)
                    ?.artifact as? EvaluationOutput
            }
            .firstOrNull() ?: throw NotFoundException("No DAG found for ${customerId} with build number ${buildNumber}")

        return convertToApiDag(output.nodes)
    }

    // TODO - We're assuming that the DAG was validated before being added to the database
    private fun convertToApiDag(nodes: List<Node>): ApiDag {
        val reverseMapping = mutableMapOf<Dataset, Int>()

        val dag = Dag.fromRaw(nodes)

        dag.forEachIndexed { i, node ->
            reverseMapping[node.output] = i
        }

        return ApiDag(dag.map { node ->
            ApiDag.Node(
                namespace = node.output.namespace,
                datasetId = node.output.datasetId,
                sources = node.inputs.map { reverseMapping[it]!! }  // Guaranteed to be present due to topological iteration order
            )
        })
    }
}
