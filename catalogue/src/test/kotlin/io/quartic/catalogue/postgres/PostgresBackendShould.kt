package io.quartic.catalogue.postgres

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.catalogue.api.model.*
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.setupDbi
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.jdbi.v3.core.Jdbi
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import java.time.Instant

class PostgresBackendShould {
    @After
    fun after() {
        DBI.open().createUpdate("DELETE FROM dataset").execute()
    }

    private val backend = PostgresBackend(DATABASE)
    private val coords = DatasetCoordinates("myNamespace", "myId")

    @Test
    fun get_config_that_was_put() {
        backend[coords] = config("Yeah")

        assertThat(backend[coords], equalTo(config("Yeah")))
    }

    @Test
    fun get_null_if_nothing_set() {
        assertThat(backend[coords], nullValue())
    }

    @Test
    fun get_latest_config_if_multiple_put() {
        backend[coords] = config("Yeah")
        backend[coords] = config("Nah")
        backend[coords] = config("Hmm")

        assertThat(backend[coords], equalTo(config("Hmm")))
    }

    @Test
    fun get_null_if_latest_event_is_delete() {
        backend[coords] = config("Yeah")
        backend.remove(coords)

        assertThat(backend[coords], nullValue())
    }

    @Test
    fun say_dataset_exists_when_it_currently_does() {
        backend[coords] = config("Yeah")

        assertTrue(coords in backend)
    }

    @Test
    fun say_dataset_doesnt_exist_when_it_currently_doesnt() {
        backend[coords] = config("Yeah")
        backend.remove(coords)

        assertFalse(coords in backend)
    }

    @Test
    fun select_correct_dataset_when_multiple_present() {
        val coords2 = coords.copy(namespace = DatasetNamespace("different"))
        val coords3 = coords.copy(id = DatasetId("different"))
        val coords4 = coords.copy(namespace = DatasetNamespace("different"), id = DatasetId("different"))

        backend[coords] = config("Yeah")
        backend[coords2] = config("Nah")
        backend[coords3] = config("Hmm")
        backend[coords4] = config("Noob")

        assertThat(backend[coords], equalTo(config("Yeah")))
    }

    @Test
    fun get_all_datasets_taking_deletions_into_account() {
        val coords2 = coords.copy(namespace = DatasetNamespace("different"))

        // Update
        backend[coords] = config("Yeah")
        backend[coords] = config("Nah")

        // Deletion
        backend[coords2] = config("Hmm")
        backend.remove(coords2)

        assertThat(backend.getAll(), equalTo(mapOf(
            coords to config("Nah")
        )))
    }

    private fun config(name: String) = DatasetConfig(
        metadata = DatasetMetadata(
            name = name,
            description = "Oh yeah baby",
            attribution = "Quartic",
            registered = Instant.EPOCH
        ),
        locator = CloudDatasetLocator(
            path = "/foo/bar/baz",
            streaming = true,
            mimeType = "application/noob+hole"
        )
    )

    companion object {
        @ClassRule
        @JvmField
        val PG = EmbeddedPostgresRules.singleInstance()

        private lateinit var DATABASE: Database
        private lateinit var DBI: Jdbi

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DATABASE = DatabaseBuilder.testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase)
            DBI = setupDbi(Jdbi.create(PG.embeddedPostgres.postgresDatabase))
        }
    }
}
