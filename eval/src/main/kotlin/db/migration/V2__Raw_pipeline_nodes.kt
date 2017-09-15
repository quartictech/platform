package db.migration

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.LexicalInfo
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node.Raw
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node.Step
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.Success
import io.quartic.eval.database.model.CurrentPhaseCompleted.Source.Bucket
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1
import io.quartic.eval.database.model.PhaseCompleted
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import java.sql.Connection

/*
 * NOTE!! This migration was previously broken.  We're retaining a fixed version purely as an example of how to use
 * legacy model types.
 */
@Suppress("unused")
class V2__Raw_pipeline_nodes : JdbcMigration {
    override fun migrate(connection: Connection) {
        val handle = setupDbi(Jdbi.create { connection }).open()

        handle.createQuery("""
            SELECT *
                FROM event
                WHERE
                    payload->>'type' = 'phase_completed' AND
                    payload#>>'{result,type}' = 'success' AND
                    payload#>>'{result,artifact,type}' = 'evaluation_output'
        """)
            .mapToMap()
            .forEach { event ->
                val oldPayload = OBJECT_MAPPER.readValue<V1>(event["payload"].toString())

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

    private fun transform(payload: V1) = PhaseCompleted(
        phaseId = payload.phaseId,
        result = Success(
            EvaluationOutput(
                transform(((payload.result as? V1.Result.Success)?.artifact as? V1.Artifact.EvaluationOutput)?.steps!!)
            )
        )
    )

    private fun transform(steps: List<V1.Step>) = calculateRaw(steps) + calculateSteps(steps)

    private fun calculateRaw(steps: List<V1.Step>): List<Node> {
        val allInputs = steps
            .flatMap { it.inputs }
            .toSet()

        val allOutputs = steps
            .flatMap { it.outputs }
            .toSet()

        val dangling = allInputs - allOutputs

        // Most of the fields can't be reconstructed
        return dangling.map {
            Raw(
                id = "0",
                info = LexicalInfo(
                    name = "missing",
                    description = "missing",
                    file = "missing",
                    lineRange = emptyList()
                ),
                output = it,
                source = Bucket(it.datasetId)
            )
        }
    }

    private fun calculateSteps(steps: List<V1.Step>): List<Node> {
        return steps.map {
            Step(
                id = it.id,
                info = LexicalInfo(
                    name = it.name,
                    description = it.description,
                    file = it.file,
                    lineRange = it.lineRange
                ),
                inputs = it.inputs,
                output = it.outputs[0]    // The DAG is only stored if it's valid, so this is fine
            )
        }
    }
}
