package io.quartic.qube

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.store.JobStore
import io.quartic.qube.store.setupDbi
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.model.ContainerSpec
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.result.ResultProducers
import org.postgresql.util.PGobject
import java.time.Instant
import java.util.*


class JobStoreShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var jobStore: JobStore
    private lateinit var dbi: Jdbi

    @Before
    fun setUp() {
        dbi = setupDbi(Jdbi.create(pg.embeddedPostgres.postgresDatabase))
        jobStore = dbi.onDemand(JobStore::class.java)
        JobStore.migrate(pg.embeddedPostgres.postgresDatabase)
    }

    @Test
    fun insert_job() {
        val uuid = UUID.randomUUID()
        val client = UUID.randomUUID()
        val request = QubeRequest.Create("blah", ContainerSpec("dummy:1", listOf("true")))
        jobStore.insertJob(
            uuid,
            client,
            "blah",
            request,
            "some log",
            Instant.now(),
            Instant.now().plusMillis(1000),
            "Completed",
            "Wat",
            0
        )

        val result = dbi.open().createQuery("select * from job")
            .execute(ResultProducers.returningResults())
            .mapToMap()
            .findFirst()
            .get()

        assertThat(result["name"] as String, equalTo("blah"))
        assertThat(
            OBJECT_MAPPER.readValue((result["create_spec"] as PGobject).value),
            equalTo(request)
        )
    }
}
