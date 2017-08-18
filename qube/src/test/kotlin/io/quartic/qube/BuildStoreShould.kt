package io.quartic.qube

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.qube.model.BuildJob
import io.quartic.qube.model.BuildPhase
import io.quartic.qube.api.model.Dag
import io.quartic.qube.model.JobResult
import io.quartic.qube.store.BuildStore
import io.quartic.qube.store.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.jdbi.v3.core.Jdbi


class BuildStoreShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var buildStore: BuildStore
    private lateinit var dbi: Jdbi

    val dag = OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)

    @Before
    fun setUp() {
        dbi = setupDbi(Jdbi.create(pg.embeddedPostgres.postgresDatabase))
        buildStore = dbi.onDemand(BuildStore::class.java)
        BuildStore.migrate(pg.embeddedPostgres.postgresDatabase)
    }

    @Test
    fun insert_build() {
        val id = buildStore.createBuild(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)
        assertThat(id, notNullValue())
    }

    @Test
    fun set_job_result() {
        val id = buildStore.createBuild(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)

        buildStore.setJobResult(
            BuildJob(id, CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST),
            JobResult(false, mapOf("my-pod" to "this is noob"), "noob hole")
        )
    }

    @Test
    fun set_dag() {
        val id = buildStore.createBuild(CustomerId(100), 100, "git", "head", "hash", BuildPhase.TEST)
        buildStore.setDag(id, dag)
        val dagOut = buildStore.getBuild(id)?.dag!!
        println(dagOut.javaClass)
        assertThat(dag, equalTo(dagOut))
    }
}
