package io.quartic.bild.store

import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildPhase
import io.quartic.common.model.CustomerId
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.Instant


interface BildDao {
    @SqlUpdate("""
        insert into build(
            customer_id,
            installation_id,
            clone_url,
            ref,
            commit,
            phase,
            start_time)
        values(:customer_id, :installation_id, :clone_url, :ref, :commit, :phase, :start_time)
    """)
    @GetGeneratedKeys
    fun createBuild(
        @Bind("customer_id") customerId: CustomerId,
        @Bind("installation_id") installationId: Long,
        @Bind("clone_url") cloneUrl: String,
        @Bind("ref") ref: String,
        @Bind("commit") commit: String,
        @Bind("phase") phase: BildPhase,
        @Bind("start_time") startTime: Instant
    ): Long

    @SqlUpdate("""
        insert into job(
            build_id,
            pod_name,
            log
        )
        values(
            :build_id,
            :pod_name,
            :log
        )
        """
    )
    fun insertJob(@Bind("build_id") buildId: BildId,
                  @Bind("pod_name") podName: String,
                  @Bind("log") log: String)

    @SqlUpdate("update build set success = :success, reason = :reason where id = :id")
    fun  setResult(@Bind("id") id: BildId, @Bind("success") success: Boolean, @Bind("reason") reason: String)

    @SqlUpdate("update build set dag = :dag where id = :id")
    fun  setDag(@Bind("id") id: BildId, @BindJson("dag") dag: Any?)
}
