package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.BindJson
import io.quartic.common.db.CustomerIdColumnMapper
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
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
    RegisterColumnMapper(Database.TriggerDetailsColumnMapper::class),
    RegisterColumnMapper(Database.BuildResultSuccessColumnMapper::class),
    RegisterColumnMapper(CustomerIdColumnMapper::class))
interface Database {
    data class BuildRow(
        val id: UUID,
        @ColumnName("customer_id")
        val customerId: CustomerId,
        val branch: String,
        @ColumnName("build_number")
        val buildNumber: Long
    )

    data class ValidDag(
        @ColumnName("payload")
        val artifact: EvaluationOutput  // TODO - eliminate the hardcoded type
    )

    class TriggerDetailsColumnMapper : ColumnMapper<TriggerDetails> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): TriggerDetails =
            OBJECT_MAPPER.readValue(r.getString(columnNumber))
    }

    class BuildResultSuccessColumnMapper : ColumnMapper<EvaluationOutput> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): EvaluationOutput {
            val payload = OBJECT_MAPPER.readValue<BuildEvent.PhaseCompleted>(r.getString(columnNumber))

            return (payload.result as Result.Success).artifact as EvaluationOutput
        }
    }


    @SqlQuery("select id, customer_id, branch, build_number from build where id = :id")
    fun getBuild(@Bind("id") id: UUID): BuildRow

    // TODO - getLatestValidDag and getLatestDag don't take PhaseCompleted != BuildSucceeded into account, nor multi-phase builds

    @SqlQuery("""
        select payload from event
        left join build on build.id = event.build_id
        where build.customer_id = :customer_id
        and event.payload @> '{"type": "phase_completed"}'
        order by event.time desc
        limit 1
        """)
    fun getLatestValidDag(@Bind("customer_id") customerId: CustomerId): ValidDag?

    @SqlQuery("""
        select payload from event
        left join build on build.id = event.build_id
        where build.customer_id = :customer_id
        and build.build_number = :build_number
        and event.payload @> '{"type": "phase_completed"}'
        """)
    fun getValidDag(
        @Bind("customer_id") customerId: CustomerId,
        @Bind("build_number") buildNumber: Long
    ): ValidDag?


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

    @SqlUpdate("insert into event(id, build_id, phase_id, time, payload) values(:id, :build_id, :phase_id, :time, :payload)")
    fun insertEvent(
        @Bind("id") id: UUID,
        @BindJson("payload") payload: BuildEvent,
        @Bind("time") time: Instant,
        @Bind("build_id") buildId: UUID,
        @Bind("phase_id") phaseId: UUID? = null
    )
}
