package io.quartic.qube.store

import io.quartic.common.model.CustomerId
import io.quartic.qube.api.model.Dag
import io.quartic.qube.model.Build
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.model.BuildId
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.RowMapper
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
import java.sql.Types

class BuildMapper: RowMapper<Build> {
    override fun map(rs: ResultSet?, ctx: StatementContext?): Build = Build(
        OBJECT_MAPPER.readValue<Dag>(rs!!.getString("dag"), Dag::class.java)
    )
}

internal class BuildIdArgumentFactory : AbstractArgumentFactory<BuildId>(Types.VARCHAR) {
    override fun build(value: BuildId, config: ConfigRegistry) =
        Argument { position, statement, ctx -> statement!!.setLong(position, value.uid.toLong()) }
}

internal class CustomerIdArgumentFactory : AbstractArgumentFactory<CustomerId>(Types.VARCHAR) {
    override fun build(value: CustomerId, config: ConfigRegistry) =
        Argument { position, statement, ctx -> statement!!.setString(position, value.uid) }
}


internal class PgObjectArgFactory : AbstractArgumentFactory<PGobject>(Types.JAVA_OBJECT) {
    override fun build(value: PGobject, config: ConfigRegistry) =
        Argument { position, statement, _ -> statement!!.setObject(position, value) }
}

class BindJsonFactory : SqlStatementCustomizerFactory {
    override fun createForParameter(annotation: Annotation?, sqlObjectType: Class<*>?, method: Method?, param: Parameter?,
                                    index: Int, paramType: Type?): SqlStatementParameterCustomizer {
        return SqlStatementParameterCustomizer { stmt, arg ->
            val jsonObject = PGobject()
            jsonObject.type = "jsonb"
            jsonObject.value = OBJECT_MAPPER.writeValueAsString(arg)
            stmt!!.bind((annotation as BindJson).name, jsonObject)
        }
    }
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
    dbi.registerArgument(BuildIdArgumentFactory())
    dbi.registerArgument(PgObjectArgFactory())
    return dbi
}
