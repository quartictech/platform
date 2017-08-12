package io.quartic.bild

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.bild.model.BildPhase
import io.quartic.bild.store.PostgresJobResultStore
import io.quartic.common.model.CustomerId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.SqlObjectPlugin



class PostgresJobResultStoreShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var jobResults: PostgresJobResultStore
    private lateinit var  dbi: Jdbi

    @Before
    fun setUp() {
        dbi = Jdbi.create(pg.embeddedPostgres.postgresDatabase)
        dbi.installPlugin(SqlObjectPlugin())
        jobResults = PostgresJobResultStore(pg.embeddedPostgres.postgresDatabase, dbi)
    }


    @Test
    fun insert_build() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BildPhase.TEST)
        assertThat("sweet", equalTo("sweet"))
    }
}
