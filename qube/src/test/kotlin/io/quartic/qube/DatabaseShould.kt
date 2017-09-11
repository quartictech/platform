package io.quartic.qube

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.setupDbi
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.model.ContainerSpec
import io.quartic.qube.api.model.ContainerState
import io.quartic.qube.api.model.PodSpec
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


class DatabaseShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var database: Database
    private lateinit var dbi: Jdbi

    @Before
    fun setUp() {
        dbi = setupDbi(Jdbi.create(pg.embeddedPostgres.postgresDatabase))
        database = DatabaseBuilder.testDao(javaClass, pg.embeddedPostgres.postgresDatabase)
    }

    @Test
    fun insert_job() {
        val uuid = UUID.randomUUID()
        val client = UUID.randomUUID()
        val request = QubeRequest.Create("blah",
            PodSpec(listOf(ContainerSpec("noob", "dummy:1", listOf("true"), 8000))))
        database.insertJob(
            uuid,
            client,
            "blah",
            request,
            Instant.now(),
            Instant.now().plusMillis(1000),
            mapOf("noob" to ContainerState(0, "wat", "wat", "logs"))
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