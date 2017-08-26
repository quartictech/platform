package io.quartic.eval.apis

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


@RegisterColumnMappers(RegisterColumnMapper(Database.TriggerDetailsColumnMapper::class), RegisterColumnMapper(CustomerIdColumnMapper::class))
interface Database {
    sealed class BuildResult {
        abstract val messages: List<QuartyMessage>
        data class Success(override val messages: List<QuartyMessage>, val dag: List<Step>) : BuildResult()
        data class InternalError(override val messages: List<QuartyMessage>, val throwable: Throwable) : BuildResult()
        data class UserError(override val messages: List<QuartyMessage>, val message: String) : BuildResult()
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

    class TriggerDetailsColumnMapper: ColumnMapper<TriggerDetails> {
        override fun map(r: ResultSet?, columnNumber: Int, ctx: StatementContext?): TriggerDetails =
            OBJECT_MAPPER.readValue(r!!.getString(columnNumber))
    }


    @SqlQuery("select id, customer_id, build_number, trigger_details, time from build where id = :id")
    fun getBuild(@Bind("id") id: UUID): Build

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
    fun insertMessage(@Bind("id") id: UUID,
                      @Bind("phase_id") phaseId: UUID,
                      @Bind("type") type: String,
                      @BindJson("message") message: QuartyMessage,
                      @Bind("time") time: Instant)

    @SqlUpdate("insert into event(id, phase_id, type, message, time) values(:id, :phase_id, :type, :message, :time)")
    fun insertTerminalMessage(@Bind("id") id: UUID?,
                              @Bind("phase_id") phaseId: UUID,
                              @Bind("type") simpleName: String?,
                              @BindJson("message") result: Database.BuildResult,
                              @Bind("time") time: Instant?)
}
