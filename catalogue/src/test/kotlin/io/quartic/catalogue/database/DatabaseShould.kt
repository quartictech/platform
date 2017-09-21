package io.quartic.catalogue.database

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.database.Database.CoordinatesAndConfig
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.setupDbi
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.jdbi.v3.core.Jdbi
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import java.time.Instant

class DatabaseShould {
    @After
    fun after() {
        DBI.open().createUpdate("DELETE FROM dataset").execute()
    }

    private val namespace = "myNamespace"
    private val id = "myId"

    private val configA = config("A")
    private val configB = config("B")

    @Test
    fun get_config_that_was_put() {
        DATABASE.insertDataset(namespace, id, configA)

        assertThat(DATABASE.getDataset(namespace, id), equalTo(configA))
    }

    @Test
    fun get_null_if_dataset_doesnt_exist() {
        assertThat(DATABASE.getDataset(namespace, id), nullValue())
    }

    @Test
    fun get_latest_config_if_multiple_put() {
        DATABASE.insertDataset(namespace, id, configA)
        DATABASE.insertDataset(namespace, id, configB)

        assertThat(DATABASE.getDataset(namespace, id), equalTo(configB))
    }

    @Test
    fun get_null_if_dataset_is_deleted() {
        DATABASE.insertDataset(namespace, id, configA)
        DATABASE.deleteDataset(namespace, id)

        assertThat(DATABASE.getDataset(namespace, id), nullValue())
    }

    @Test
    fun select_correct_dataset_when_multiple_present() {
        DATABASE.insertDataset(namespace, id, configA)
        DATABASE.insertDataset("different", "alternative", configB)

        assertThat(DATABASE.getDataset(namespace, id), equalTo(configA))
    }

    @Test
    fun get_all_datasets() {
        DATABASE.insertDataset(namespace, id, configA)
        DATABASE.insertDataset("different", "alternative", configB)

        assertThat(DATABASE.getDatasets(), equalTo(listOf(
            CoordinatesAndConfig(namespace, id, configA),
            CoordinatesAndConfig("different", "alternative", configB)
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
