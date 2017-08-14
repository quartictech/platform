package io.quartic.bild

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.model.JobResult
import io.quartic.bild.store.PostgresJobResultStore
import io.quartic.common.model.CustomerId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.jdbi.v3.core.Jdbi


class PostgresJobResultStoreShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var jobResults: PostgresJobResultStore
    private lateinit var  dbi: Jdbi

    @Before
    fun setUp() {
        dbi = Jdbi.create(pg.embeddedPostgres.postgresDatabase)
        dbi = io.quartic.bild.store.dbi(dbi)
        jobResults = PostgresJobResultStore(pg.embeddedPostgres.postgresDatabase, dbi)
    }


    @Test
    fun insert_build() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)

        assertThat(id, notNullValue())
    }

    @Test
    fun set_job_result() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)

        jobResults.putJobResult(
            BuildJob(id, CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST),
            JobResult(false, mapOf("my-pod" to "this is noob"), "noob hole")
        )
    }

    @Test
    fun put_dag() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)
        jobResults.putDag(id, mapOf("foo" to "bar"))
    }
}
