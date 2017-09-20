package io.quartic.catalogue.postgres

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.postgres.Database.ConfigColumnMapper
import io.quartic.catalogue.postgres.Database.DatasetRowMapper
import io.quartic.common.db.BindJson
import io.quartic.common.serdes.OBJECT_MAPPER
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.sql.ResultSet

@RegisterRowMapper(DatasetRowMapper::class)
@RegisterColumnMapper(ConfigColumnMapper::class)
interface Database {

    data class CoordinatesAndConfig(
        val namespace: String,
        val id: String,
        val config: DatasetConfig?
    )

    class DatasetRowMapper : RowMapper<DatasetConfig> {
        override fun map(rs: ResultSet, ctx: StatementContext) =
            OBJECT_MAPPER.readValue<DatasetConfig>(rs.getString("config"))
    }

    class ConfigColumnMapper : ColumnMapper<DatasetConfig> {
        override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) =
            OBJECT_MAPPER.readValue<DatasetConfig>(r.getString(columnNumber))
    }

    @SqlUpdate("""
        INSERT INTO dataset (namespace, id, config)
            VALUES (:namespace, :id, :config)
            ON CONFLICT (namespace, id)
            DO UPDATE SET config = :config
    """)
    fun insertDataset(
        @Bind("namespace") namespace: String,
        @Bind("id") id: String,
        @BindJson("config") config: DatasetConfig
    )

    @SqlUpdate("""
        DELETE FROM dataset
            WHERE
                namespace = :namespace AND
                id = :id
    """)
    fun deleteDataset(
        @Bind("namespace") namespace: String,
        @Bind("id") id: String
    )

    @SqlQuery("""
        SELECT config FROM dataset
            WHERE
                namespace = :namespace AND
                id = :id
    """)
    fun getDataset(
        @Bind("namespace") namespace: String,
        @Bind("id") id: String
    ): DatasetConfig?

    @SqlQuery("""
        SELECT * FROM dataset
    """)
    fun getDatasets(): List<CoordinatesAndConfig>
}
