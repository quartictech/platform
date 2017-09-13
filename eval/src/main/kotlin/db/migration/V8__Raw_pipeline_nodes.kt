package db.migration

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import java.sql.Connection

class V8__Raw_pipeline_nodes : JdbcMigration {
    override fun migrate(connection: Connection) {


        val handle = Jdbi.open(connection)

        handle.createQuery("""
            SELECT *
                FROM event
                WHERE payload->>'type' = 'phase_completed_v1'
        """)
            .mapToMap()
            .forEach {
                val payload = OBJECT_MAPPER.readValue<Map<String, *>>(it["payload"]!!.toString())

                if (payload["result"]?["type"]?["artifact"] == "evaluation_output") {

            }
            }
    }
}
