package io.quartic.eval.apis

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.db.BindJson
import io.quartic.db.CustomerIdColumnMapper
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.Step
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
    RegisterColumnMapper(Database.DagColumnMapper::class),
    RegisterColumnMapper(CustomerIdColumnMapper::class))
interface Database {
    @JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
    sealed class BuildResult {
        @JsonTypeName("success")
        data class Success(val dag: List<Step>) : BuildResult()
        @JsonTypeName("internal_error")
        data class InternalError(val throwable: Throwable) : BuildResult()
        @JsonTypeName("user_error")
        data class UserError(val detail: Any?) : BuildResult()
    }

    data class Build(
        val id: UUID,
        @ColumnName("customer_id")
        val customerId: CustomerId,
        @ColumnName("build_number")
        val buildNumber: Long,
        @ColumnName("trigger_details")
        val triggerDetails: TriggerDetails,
        val time: Instant
    )

    data class Dag(
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
        override fun map(r: ResultSet?, columnNumber: Int, ctx: StatementContext?): TriggerDetails =
            OBJECT_MAPPER.readValue(r!!.getString(columnNumber))
    }

    class DagColumnMapper: ColumnMapper<BuildResult.Success> {
        override fun map(r: ResultSet?, columnNumber: Int, ctx: StatementContext?): BuildResult.Success =
            OBJECT_MAPPER.readValue(r!!.getString(columnNumber))
    }


    @SqlQuery("select id, customer_id, build_number, trigger_details, time from build where id = :id")
    fun getBuild(@Bind("id") id: UUID): Build

    @SqlQuery("""
        select message from event
        left join phase on phase.id = event.phase_id
        left join build on build.id = phase.build_id
        where build.customer_id = :customer_id
        and event.type = 'SUCCESS'
        order by event.time desc
        limit 1
        """)
    fun getLatestDag(@Bind("customer_id") customerId: CustomerId): Dag

    @SqlUpdate("""
        with next as (select coalesce(max(build_number), 0) + 1 as build_number from build where customer_id=:customer_id)
        insert into build(id, customer_id, build_number, trigger_details, time)
        select :id, :customer_id, next.build_number, :trigger_details, :time
        from next
        """)
    fun insertBuild(@Bind("id") id: UUID,
                    @Bind("customer_id") customerId: CustomerId,
                    @BindJson("trigger_details") triggerDetails: TriggerDetails,
                    @Bind("time") time: Instant)

    @SqlUpdate("insert into phase(id, build_id, name, time) values(:id, :build_id, :name, :time)")
    fun insertPhase(@Bind("id") id: UUID,
                    @Bind("build_id") buildId: UUID,
                    @Bind("name") name: String,
                    @Bind("time") startTime: Instant)

    @SqlUpdate("insert into event(id, phase_id, type, message, time) values(:id, :phase_id, :type, :message, :time)")
    fun insertEvent(@Bind("id") id: UUID,
                    @Bind("phase_id") phaseId: UUID,
                    @Bind("type") type: EventType,
                    @BindJson("message") message: QuartyMessage,
                    @Bind("time") time: Instant)

    @SqlUpdate("insert into event(id, phase_id, type, message, time) values(:id, :phase_id, :type, :message, :time)")
    fun insertTerminalEvent(@Bind("id") id: UUID?,
                            @Bind("phase_id") phaseId: UUID,
                            @Bind("type") type: EventType,
                            @BindJson("message") result: BuildResult,
                            @Bind("time") time: Instant?)
}
