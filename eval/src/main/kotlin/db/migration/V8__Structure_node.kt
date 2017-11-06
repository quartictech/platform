package db.migration

import io.quartic.eval.database.PayloadMigration
import io.quartic.eval.database.model.LegacyPhaseCompleted
import io.quartic.eval.database.model.PhaseCompletedV8

@Suppress("unused")
class V8__Structure_node : PayloadMigration<LegacyPhaseCompleted.V6, PhaseCompletedV8>(
    LegacyPhaseCompleted.V6::class.java,
    PhaseCompletedV8::class.java
) {
    override fun sql(): String =
        """
            SELECT *
                FROM event
                WHERE
                    payload->>'type' = 'phase_completed'
        """

    override fun transform(payload: LegacyPhaseCompleted.V6) = PhaseCompletedV8(
        payload.phaseId,
        transformResult(payload.result)
    )

    private fun transformResult(result: LegacyPhaseCompleted.V6.Result) = when (result) {
        is LegacyPhaseCompleted.V6.Result.Success -> PhaseCompletedV8.Result.Success(
            if (result.artifact != null) transformArtifact(result.artifact) else null
        )
        is LegacyPhaseCompleted.V6.Result.InternalError -> PhaseCompletedV8.Result.InternalError()
        is LegacyPhaseCompleted.V6.Result.UserError -> PhaseCompletedV8.Result.UserError(transformUserErrorInfo(result.info))
    }

    private fun transformArtifact(artifact: LegacyPhaseCompleted.V6.Artifact) = when (artifact) {
        is LegacyPhaseCompleted.V6.Artifact.EvaluationOutput -> transformEvaluationOutput(artifact)
        is LegacyPhaseCompleted.V6.Artifact.NodeExecution -> PhaseCompletedV8.Artifact.NodeExecution(artifact.skipped)
    }

    private fun transformUserErrorInfo(info: LegacyPhaseCompleted.V5.UserErrorInfo) = when (info) {
        is LegacyPhaseCompleted.V5.UserErrorInfo.InvalidDag ->
            PhaseCompletedV8.UserErrorInfo.InvalidDag(
                info.error,
                info.nodes.map { node -> transformNode(node) }
            )
        is LegacyPhaseCompleted.V5.UserErrorInfo.OtherException ->
            PhaseCompletedV8.UserErrorInfo.OtherException(info.detail)
    }

    private fun transformEvaluationOutput(artifact: LegacyPhaseCompleted.V6.Artifact.EvaluationOutput) =
        PhaseCompletedV8.Artifact.EvaluationOutput(artifact.nodes.map { node -> transformNode(node) })

    private fun transformNode(node: LegacyPhaseCompleted.V2.Node) = when (node) {
        is LegacyPhaseCompleted.V2.Node.Raw -> PhaseCompletedV8.Node.Raw(
            node.id,
            node.id,
            mapOf(),
            node.info,
            node.source,
            node.output
        )
        is LegacyPhaseCompleted.V2.Node.Step -> PhaseCompletedV8.Node.Step(
            node.id,
            node.id,
            mapOf(),
            node.info,
            node.inputs,
            node.output
        )
    }
}
