package io.quartic.common.db

import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.postgresql.util.PGobject
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.Type
import java.sql.ResultSet

fun <T : SqlStatement<*>> T.bindJson(name: String, o: Any): T {
    val pgObject = PGobject()
    pgObject.type = "jsonb"
    pgObject.value = OBJECT_MAPPER.writeValueAsString(o)
    this.bind(name, pgObject)
    return this
}

class BindJsonFactory : SqlStatementCustomizerFactory {
    override fun createForParameter(annotation: Annotation?, sqlObjectType: Class<*>?, method: Method?, param: Parameter?,
                                    index: Int, paramType: Type?): SqlStatementParameterCustomizer {
        return SqlStatementParameterCustomizer { stmt, arg ->
            stmt!!.bindJson((annotation as BindJson).name, arg)
        }
    }
}

class CustomerIdColumnMapper : ColumnMapper<CustomerId> {
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): CustomerId = CustomerId(r.getLong(columnNumber))
}


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@SqlStatementCustomizingAnnotation(BindJsonFactory::class)
annotation class BindJson(val name: String)

fun setupDbi(dbi: Jdbi): Jdbi {
    dbi.installPlugin(SqlObjectPlugin())
    dbi.installPlugin(KotlinPlugin())
    dbi.installPlugin(KotlinSqlObjectPlugin())
    dbi.installPlugin(PostgresPlugin())
    dbi.registerArgument(CustomerIdArgumentFactory())
    dbi.registerArgument(PgObjectArgFactory())
    return dbi
}


