package db.migration

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.CurrentPhaseCompleted
import io.quartic.eval.database.model.LegacyPhaseCompleted
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import java.sql.Connection

/*
 * NOTE!! This migration was previously broken.  We're retaining a fixed version purely as an example of how to use
 * legacy model types.
 */
@Suppress("unused")
class V5__Structure_user_errors : JdbcMigration {
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
                val oldPayload = OBJECT_MAPPER.readValue<LegacyPhaseCompleted.V3>(event["payload"].toString())

                val newPayload = transform(oldPayload)

                handle.createUpdate("""
                    UPDATE event
                        SET payload = :payload
                        WHERE id = :id AND
                              payload#>>'{result,type}' = 'user_error'
                """)
                    .bind("id", event["id"])
                    .bindJson("payload", newPayload)
                    .execute()
            }
    }

    private fun transform(event: LegacyPhaseCompleted.V3) = CurrentPhaseCompleted(
        event.phaseId,
        CurrentPhaseCompleted.Result.UserError(
            CurrentPhaseCompleted.UserErrorInfo.OtherException(
                when (event.result) {
                    is LegacyPhaseCompleted.V3.Result.UserError ->
                        CurrentPhaseCompleted.UserErrorInfo.OtherException(event.result.detail)
                    else -> throw IllegalStateException("Can only transform UserError")
                }
            )
        )
    )
}
