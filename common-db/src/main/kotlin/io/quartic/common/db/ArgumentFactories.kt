package io.quartic.common.db

import io.quartic.common.model.CustomerId
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.postgresql.util.PGobject
import java.sql.Types

internal class CustomerIdArgumentFactory : AbstractArgumentFactory<CustomerId>(Types.VARCHAR) {
    override fun build(value: CustomerId, config: ConfigRegistry) =
        Argument { position, statement, _ -> statement!!.setLong(position, value.uid.toLong()) }
}

internal class PgObjectArgFactory : AbstractArgumentFactory<PGobject>(Types.JAVA_OBJECT) {
    override fun build(value: PGobject, config: ConfigRegistry) =
        Argument { position, statement, _ -> statement!!.setObject(position, value) }
}
