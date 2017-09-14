package io.quartic.eval.database

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.setupDbi
import org.flywaydb.core.api.MigrationVersion
import org.jdbi.v3.core.Jdbi
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DatabaseMigrationsShould {
    @Test
    fun v1_migrate() {
        databaseVersion("1")
    }

    private fun databaseVersion(version: String): Database = DatabaseBuilder
        .testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase,
            MigrationVersion.fromVersion(version))

    companion object {
        @ClassRule
        @JvmField
        val PG = EmbeddedPostgresRules.singleInstance()

        private lateinit var DBI: Jdbi

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DBI = setupDbi(Jdbi.create(PG.embeddedPostgres.postgresDatabase))
        }
    }
}
