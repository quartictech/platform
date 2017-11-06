package io.quartic.eval.database

import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.jdbi.v3.core.Jdbi
import java.sql.Connection

abstract class PayloadMigration<in Old, New>(private val oldClass: Class<Old>,
                                             val newClass: Class<New>) : JdbcMigration {
    override fun migrate(connection: Connection?) {
         val handle = setupDbi(Jdbi.create { connection }).open()

        handle.createQuery(sql())
            .mapToMap()
            .forEach { event ->
                val oldPayload: Old = OBJECT_MAPPER.readValue(event["payload"].toString(), oldClass)

                val newPayload: New = transform(oldPayload)

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

    abstract protected fun sql(): String
    abstract protected fun transform(payload: Old): New
}
