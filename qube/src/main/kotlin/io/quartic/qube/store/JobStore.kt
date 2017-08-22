package io.quartic.qube.store

import io.quartic.qube.api.Request
import org.flywaydb.core.Flyway
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.Instant
import java.util.*
import javax.sql.DataSource

/**
 * Job here represents a run of a Kubernetes Pod and *not* an actual Kubernetes Job.
 */
@RegisterRowMapper(BuildMapper::class)
interface JobStore {
    @SqlUpdate("""insert into job(
        id,
        client,
        name,
        create_spec,
        log,
        start_time,
        end_time,
        reason,
        message,
        exit_code
    ) values(
        :id,
        :client,
        :name,
        :create_spec,
        :log,
        :start_time,
        :end_time,
        :reason,
        :message,
        :exit_code)""")
    fun insertJob(
        @Bind("id") id: UUID,
        @Bind("client") client: UUID,
        @Bind("name") podName: String,
        @BindJson("create_spec") createPod: Request.CreatePod,
        @Bind("log") log: String?,
        @Bind("start_time") startTime: Instant,
        @Bind("end_time") endTime: Instant,
        @Bind("reason") reason: String?,
        @Bind("message") message: String?,
        @Bind("exit_code") exitCode: Int?
    )

    companion object {
        fun migrate(database: DataSource) {
            val flyway = Flyway()
            flyway.dataSource = database
            flyway.migrate()
        }
    }
}
