package io.quartic.eval.database

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.BindJson
import io.quartic.common.db.CustomerIdColumnMapper
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.Database.*
import io.quartic.eval.database.model.BuildEvent
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.Success
import io.quartic.eval.database.model.PhaseCompleted
import io.quartic.eval.database.model.TriggerReceived
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper
import org.jdbi.v3.sqlobject.config.RegisterColumnMappers
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.sql.ResultSet
import java.time.Instant
import java.util.*


@RegisterColumnMappers(
    RegisterColumnMapper(TriggerReceivedColumnMapper::class),
    RegisterColumnMapper(BuildResultSuccessColumnMapper::class),
    RegisterColumnMapper(CustomerIdColumnMapper::class),
    RegisterColumnMapper(BuildEventColumnMapper::class))
interface Database {

    data class BuildRow(
        val id: UUID,
        @ColumnName("customer_id")
        val customerId: CustomerId,
        val branch: String,
        @ColumnName("build_number")
        val buildNumber: Long
    )

    data class EventRow(
        val id: UUID,
        @ColumnName("build_id")
        val buildId: UUID,
        val time: Instant,
        val payload: BuildEvent
    )

    data class BuildStatusRow(
        val id: UUID,
        @ColumnName("build_number")
        val buildNumber: Long,
        val branch: String,
        @ColumnName("customer_id")
        val customerId: CustomerId,
        val status: String,
        val time: Instant,
        val trigger: TriggerReceived
    )

    class TriggerReceivedColumnMapper : ColumnMapper<TriggerReceived> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): TriggerReceived =
            OBJECT_MAPPER.readValue(r.getString(columnNumber))
    }

    class BuildResultSuccessColumnMapper : ColumnMapper<EvaluationOutput> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): EvaluationOutput {
            val payload = OBJECT_MAPPER.readValue<PhaseCompleted>(r.getString(columnNumber))

            return (payload.result as Success).artifact as EvaluationOutput
        }
    }

    class BuildEventColumnMapper : ColumnMapper<BuildEvent> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): BuildEvent =
            OBJECT_MAPPER.readValue(r.getString(columnNumber))
    }


    @SqlQuery("select id, customer_id, branch, build_number from build where id = :id")
    fun getBuild(@Bind("id") id: UUID): BuildRow

    @SqlQuery("""
        SELECT * FROM event
            LEFT JOIN build ON build.id = event.build_id
            WHERE
                build.customer_id = :customer_id AND
                build.build_number = :build_number
            ORDER BY event.time ASC
        """)
    fun getEventsForBuild(
        @Bind("customer_id") customerId: CustomerId,
        @Bind("build_number") buildNumber: Long
    ): List<EventRow>

    @SqlQuery("""
        SELECT build_number FROM build
            LEFT JOIN event on build.id = event.build_id
            WHERE event.payload @> '{"type": "build_succeeded"}'
            ORDER BY event.time DESC
            LIMIT 1
        """)
    fun getLatestSuccessfulBuildNumber(
        @Bind("customer_id") customerId: CustomerId
    ): Long?


    @SqlUpdate("""
        with next as (select coalesce(max(build_number), 0) + 1 as build_number from build where customer_id=:customer_id)
        insert into build(id, customer_id, branch, build_number)
        select :id, :customer_id, :branch, next.build_number
        from next
        """)
    fun insertBuild(
        @Bind("id") id: UUID,
        @Bind("customer_id") customerId: CustomerId,
        @Bind("branch") branch: String
    )

    @SqlUpdate("insert into event(id, build_id, time, payload) values(:id, :build_id, :time, :payload)")
    fun insertEvent(
        @Bind("id") id: UUID,
        @BindJson("payload") payload: BuildEvent,
        @Bind("time") time: Instant,
        @Bind("build_id") buildId: UUID
    )

    @SqlQuery("""
        SELECT
            build.*,
            COALESCE(
                (
                    CASE eterm.payload->>'type'
                        WHEN 'build_succeeded' THEN 'success'
                        WHEN 'build_failed' THEN 'failure'
                    END
                ),
                'running') AS status,
            etrigger.time AS time,
            etrigger.payload as trigger
        FROM build
        LEFT JOIN event eterm ON (
            eterm.build_id = build.id AND
            (
                eterm.payload @> '{"type": "build_succeeded"}' OR
                eterm.payload @> '{"type": "build_failed"}'
            )
        )
        LEFT JOIN event etrigger ON etrigger.build_id = build.id
        WHERE
            etrigger.payload @> '{"type": "trigger_received"}' AND
            build.customer_id = :customer_id AND
            (:build_number is null OR build.build_number = :build_number)
        ORDER BY etrigger.time DESC
        LIMIT 20
        """)
    fun getBuilds(
        @Bind("customer_id") customerId: CustomerId,
        @Bind("build_number") buildNumber: Long? = null
    ): List<BuildStatusRow>
}