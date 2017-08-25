package io.quartic.eval.apis

import io.quartic.common.model.CustomerId
import io.quartic.db.BindJson
import io.quartic.quarty.model.Step
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.util.*


interface Database {
    sealed class BuildResult {
        data class Success(val dag: List<Step>) : BuildResult()
        data class InternalError(val throwable: Throwable) : BuildResult()
        data class UserError(val message: String) : BuildResult()
    }

    @SqlQuery("select dag from build where customer_id = :customer_id")
    fun getBuildResult(@Bind("customer_id") customerId: CustomerId): BuildResult

    @SqlUpdate("insert into build(id, customer_id, result) values(:id, :customer_id, :result)")
    fun writeResult(@Bind("id") uuid: UUID,
                    @Bind("customer_id") customerId: CustomerId,
                    @BindJson("result") result: BuildResult)
}
