package io.quartic.bild.store

import io.quartic.common.model.CustomerId
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import java.sql.Types

internal class CustomerIdArgumentFactory : AbstractArgumentFactory<CustomerId>(Types.VARCHAR) {
    override fun build(value: CustomerId, config: ConfigRegistry): Argument {
        return Argument { position, statement, ctx -> statement!!.setString(position, value.uid) }
    }
}

fun dbi(dbi: Jdbi): Jdbi {
    dbi.installPlugin(SqlObjectPlugin())
    dbi.installPlugin(KotlinPlugin())
    dbi.registerArgument(CustomerIdArgumentFactory())
    return dbi
}
