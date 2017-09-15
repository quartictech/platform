package db.migration

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

            }
    }
}
