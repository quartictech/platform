package io.quartic.bild.store

import io.quartic.bild.api.model.Dag
import io.quartic.bild.model.*
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource





@RegisterRowMapper(BuildMapper::class)
interface JobStore {
    fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String,
                           ref: String, commit: String, phase: BuildPhase): BuildId {
        return BuildId(insertBuild(customerId, installationId, cloneUrl, ref,
            commit, phase, Instant.now()).toString())
    }

    @SqlUpdate("""insert into build(customer_id, installation_id, clone_url, ref, commit, phase, start_time)
        values(:customer_id, :installation_id, :clone_url, :ref, :commit, :phase, :start_time)""")
    @GetGeneratedKeys
    fun insertBuild(
        @Bind("customer_id") customerId: CustomerId,
        @Bind("installation_id") installationId: Long,
        @Bind("clone_url") cloneUrl: String,
        @Bind("ref") ref: String,
        @Bind("commit") commit: String,
        @Bind("phase") phase: BuildPhase,
        @Bind("start_time") startTime: Instant
    ): Long

    @SqlUpdate("""insert into job(build_id, pod_name, log) values(:build_id, :pod_name, :log)""")
    fun insertJob(@Bind("build_id") buildId: BuildId,
                  @Bind("pod_name") podName: String,
                  @Bind("log") log: String)

    @SqlUpdate("update build set success = :success, reason = :reason where id = :id")
    fun setResult(@Bind("id") id: BuildId, @Bind("success") success: Boolean, @Bind("reason") reason: String)

    @SqlUpdate("update build set dag = :dag where id = :id")
    fun setDag(@Bind("id") id: BuildId, @BindJson("dag") dag: Dag?)

    @SqlQuery("select id from build where customer_id = :customer_id order by start_time desc limit 1")
    fun getLatest(@Bind("customer_id") customerId: CustomerId): BuildId?

    @SqlQuery("select * from build where id = :id")
    fun getBuild(@Bind("id") id: BuildId): Build?

    @Transaction
    fun setJobResult(job: BuildJob, jobResult: JobResult) {
        jobResult.logOutputByPod.forEach { podName, log -> insertJob(job.id, podName, log) }
        setResult(job.id, jobResult.success, jobResult.reason)
    }

    companion object {
        fun migrate(database: DataSource) {
            val flyway = Flyway()
            flyway.dataSource = database
            flyway.migrate()
        }
    }
}
