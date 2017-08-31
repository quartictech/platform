package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.db.BindJson
import io.quartic.common.db.CustomerIdColumnMapper
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildResult
import io.quartic.quarty.model.QuartyMessage
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

    data class BuildResultSuccessRow(
        @ColumnName("message")
        val message: BuildResult.Success
    )

    enum class EventType {
        MESSAGE,
        SUCCESS,
        INTERNAL_ERROR,
        USER_ERROR
    }

    class TriggerDetailsColumnMapper: ColumnMapper<TriggerDetails> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): TriggerDetails =
            OBJECT_MAPPER.readValue(r.getString(columnNumber))
    }

    class BuildResultSuccessColumnMapper: ColumnMapper<BuildResult.Success> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): BuildResult.Success {
            val buildResult = OBJECT_MAPPER.readValue<BuildResult.Success>(r.getString(columnNumber))
            buildResult.checkVersion()
            return buildResult
        }
    }


    @SqlQuery("select id, customer_id, branch, build_number from build where id = :id")
    fun getBuild(@Bind("id") id: UUID): BuildRow

    @SqlQuery("""
        select message from event
        left join build on build.id = event.build_id
        where build.customer_id = :customer_id
        and event.type = 'SUCCESS'
        order by event.time desc
        limit 1
        """)
    fun getLatestSuccess(@Bind("customer_id") customerId: CustomerId): BuildResultSuccessRow?

    @SqlQuery("""
        select message from event
        left join phase on phase.id = event.phase_id
        left join build on build.id = phase.build_id
        where build.customer_id = :customer_id and
        build.build_number = :build_number
        and event.type = 'SUCCESS'
        """)
    fun getSuccess(@Bind("customer_id") customerId: CustomerId,
                   @Bind("build_number") buildNumber: Long): BuildResultSuccessRow?


    @SqlUpdate("insert into event(id, build_id, time, payload) values(:id, :build_id, :time, :payload)")
    fun insertEvent2(
        @Bind("id") id: UUID,
        @Bind("build_id") buildId: UUID,
        @Bind("time") time: Instant,
        @BindJson("payload") payload: BuildEvent
    )


    /////////////////////////////////////

    @SqlUpdate("""
        with next as (select coalesce(max(build_number), 0) + 1 as build_number from build where customer_id=:customer_id)
        insert into build(id, customer_id, branch, build_number)
        select :id, :customer_id, :branch, next.build_number
        from next
        """)
    fun insertBuild(@Bind("id") id: UUID,
                    @Bind("customer_id") customerId: CustomerId,
                    @Bind("branch") branch: String)

    @SqlUpdate("insert into event(id, build_id, phase_id, type, message, time) values(:id, :build_id, :phase_id, :type, :message, :time)")
    fun insertEvent(@Bind("id") id: UUID,
                    @Bind("build_id") buildId: UUID,
                    @Bind("phase_id") phaseId: UUID,
                    @Bind("type") type: EventType,
                    @BindJson("message") message: QuartyMessage,
                    @Bind("time") time: Instant)

    @SqlUpdate("insert into event(id, build_id, phase_id, type, message, time) values(:id, :build_id, :phase_id, :type, :message, :time)")
    fun insertTerminalEvent(@Bind("id") id: UUID,
                            @Bind("build_id") buildId: UUID,
                            @Bind("phase_id") phaseId: UUID,
                            @Bind("type") type: EventType,
                            @BindJson("message") result: BuildResult,
                            @Bind("time") time: Instant)
}
