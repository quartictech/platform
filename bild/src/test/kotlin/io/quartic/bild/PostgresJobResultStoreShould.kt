package io.quartic.bild

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.bild.store.PostgresJobResultStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.skife.jdbi.v2.DBI
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*

class PostgresJobResultStoreShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var jobResults: PostgresJobResultStore
    private lateinit var  dbi: DBI

    @Before
    fun setUp() {
        dbi = DBI(pg.embeddedPostgres.postgresDatabase)
        jobResults = PostgresJobResultStore(pg.embeddedPostgres.postgresDatabase, dbi)
    }


    @Test
    fun fetch_bild() {
        assertThat("sweet", equalTo("sweet"))
    }
}
