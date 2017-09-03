package db.migration

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.toTriggerReceived
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import org.postgresql.util.PGobject
import java.sql.Connection

class V6__Migrate_trigger_details : JdbcMigration {
    override fun migrate(connection: Connection?) {
        val dbi = setupDbi(Jdbi.create({ connection }))
        val handle = dbi.open()
        handle.createQuery("SELECT id, event.payload->'details' as json FROM event WHERE event.payload->>'type' = 'trigger_received_v1'")
            .mapToMap()
            .forEach { map ->
                val triggerDetails: TriggerDetails = OBJECT_MAPPER.readValue((map["json"] as PGobject).value)
                handle.createUpdate("UPDATE event SET payload = :json WHERE id = :id")
                    .bindJson("json", triggerDetails.toTriggerReceived())
                    .bind("id", map["id"])
                    .execute()
            }
    }
}
