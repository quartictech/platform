package db.migration

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.LegacyPhaseCompleted
import io.quartic.eval.database.model.PhaseCompletedV8
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import java.sql.Connection

@Suppress("unused")
class V8__Structure_node : JdbcMigration {
    override fun migrate(connection: Connection) {
        val handle = setupDbi(Jdbi.create { connection }).open()

        handle.createQuery("""
            SELECT *
                FROM event
                WHERE
                    payload->>'type' = 'phase_completed'
        """)
            .mapToMap()
            .forEach { event ->
                val oldPayload = OBJECT_MAPPER.readValue<LegacyPhaseCompleted.V6>(event["payload"].toString())

                val newPayload = transform(oldPayload)

                handle.createUpdate("""
                    UPDATE event
                        SET payload = :payload
                        WHERE id = :id
                """)
                    .bind("id", event["id"])
                    .bindJson("payload", newPayload)
                    .execute()
            }
    }

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

    private fun transformArtifact(artifact: LegacyPhaseCompleted.V6.Artifact) = when (artifact) {
        is LegacyPhaseCompleted.V6.Artifact.EvaluationOutput -> transformEvaluationOutput(artifact)
        is LegacyPhaseCompleted.V6.Artifact.NodeExecution -> PhaseCompletedV8.Artifact.NodeExecution(artifact.skipped)
    }

    private fun transformResult(result: LegacyPhaseCompleted.V6.Result) = when (result) {
        is LegacyPhaseCompleted.V6.Result.Success -> PhaseCompletedV8.Result.Success(
            if (result.artifact != null) transformArtifact(result.artifact) else null
        )
        is LegacyPhaseCompleted.V6.Result.InternalError -> PhaseCompletedV8.Result.InternalError()
        is LegacyPhaseCompleted.V6.Result.UserError -> PhaseCompletedV8.Result.UserError(transformUserErrorInfo(result.info))
    }

    private fun transform(event: LegacyPhaseCompleted.V6) = PhaseCompletedV8(
        event.phaseId,
        transformResult(event.result)
    )
}
