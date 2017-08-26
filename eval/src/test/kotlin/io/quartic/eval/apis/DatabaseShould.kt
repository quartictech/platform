package io.quartic.eval.apis

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.db.DatabaseBuilder
import io.quartic.eval.api.model.TriggerDetails
import org.glassfish.jersey.internal.inject.Custom
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.time.Instant
import org.hamcrest.MatcherAssert.*
import org.hamcrest.CoreMatchers.*
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import java.util.*

class DatabaseShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var database: Database

    @Before
    fun setUp() {
        database = DatabaseBuilder.testDao(Database::class.java, pg.embeddedPostgres.postgresDatabase)
    }

    @Test
    fun insert_build() {
        val id = UUID.randomUUID()
        val time = Instant.now()
        database.insertBuild(id, CustomerId(100L), triggerDetails, time)
        assertThat(database.getBuild(id), equalTo(
            Database.Build(id, CustomerId(100), 1, triggerDetails, time)
        ))
    }

    @Test
    fun use_sequential_build_numbers_per_customer() {
        val customerA = CustomerId(100L)
        val customerB = CustomerId(101L)

        (1..10).forEach { count ->
            val idA = UUID.randomUUID()
            database.insertBuild(idA, customerA, triggerDetails, Instant.now())
            assertThat(database.getBuild(idA).buildNumber, equalTo(count.toLong()))
        }

        (1..5).forEach { count ->
            val idB = UUID.randomUUID()
            database.insertBuild(idB, customerB, triggerDetails, Instant.now())
            assertThat(database.getBuild(idB).buildNumber, equalTo(count.toLong()))
        }
    }

    @Test
    fun disallow_duplicate_build_ids() {
        val id = UUID.randomUUID()
        database.insertBuild(id, CustomerId(100), triggerDetails, Instant.now())

        assertThrows<UnableToExecuteStatementException> {
            database.insertBuild(id, CustomerId(100), triggerDetails, Instant.now())
        }
    }


    val triggerDetails = TriggerDetails("wat", "id", 100, 100, URI.create("git"), "ref", "w", Instant.now())
}
