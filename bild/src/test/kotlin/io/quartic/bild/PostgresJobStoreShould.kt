package io.quartic.bild

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.api.model.Dag
import io.quartic.bild.model.JobResult
import io.quartic.bild.store.JobStore
import io.quartic.bild.store.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.jdbi.v3.core.Jdbi


class PostgresJobStoreShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var jobResults: JobStore
    private lateinit var dbi: Jdbi

    val dag = OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)

    @Before
    fun setUp() {
        dbi = setupDbi(Jdbi.create(pg.embeddedPostgres.postgresDatabase))
        jobResults = dbi.onDemand(JobStore::class.java)
        JobStore.migrate(pg.embeddedPostgres.postgresDatabase)
    }

    @Test
    fun insert_build() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)
        assertThat(id, notNullValue())
    }

    @Test
    fun set_job_result() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)

        jobResults.setJobResult(
            BuildJob(id, CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST),
            JobResult(false, mapOf("my-pod" to "this is noob"), "noob hole")
        )
    }

    @Test
    fun set_dag() {
        val id = jobResults.createJob(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)
        jobResults.setDag(id, dag)
        val dagOut = jobResults.getBuild(id)?.dag!!
        println(dagOut.javaClass)
        assertThat(dag, equalTo(dagOut))
    }
}
