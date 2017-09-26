package db.migration

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.LegacyPhaseCompleted.V4
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import java.sql.Connection

@Suppress("unused")
class V5__Structure_user_errors : JdbcMigration {
    override fun migrate(connection: Connection) {
        val handle = setupDbi(Jdbi.create { connection }).open()

        handle.createQuery("""
            SELECT *
                FROM event
                WHERE
                    payload->>'type' = 'phase_completed' AND
                    payload#>>'{result,type}' = 'user_error'
        """)
            .mapToMap()
            .forEach { event ->
                val oldPayload = OBJECT_MAPPER.readValue<V4>(event["payload"].toString())

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

    private fun transform(event: V4) = V5(
        event.phaseId,
        V5.Result.UserError(
            V5.UserErrorInfo.OtherException(
                when (event.result) {
                    is V4.Result.UserError -> V5.UserErrorInfo.OtherException(event.result.detail)
                    else -> throw IllegalStateException("Can only transform UserError. Found ${event.result}")
                }
            )
        )
    )
}
