package io.quartic.catalogue

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.client.ClientBuilder
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.util.Collections.emptyMap

class CatalogueApplicationShould {

    @Test
    fun retrieve_registered_datasets() {
        val catalogue = ClientBuilder(this).retrofit<CatalogueClient>("http://localhost:" + RULE.localPort + "/api")

        val config = DatasetConfig(
                DatasetMetadata("Foo", "Bar", "Arlo", null),
                DatasetLocator.PostgresDatasetLocator("a", "b", "c", "d"),
                emptyMap()
        )

        val coords = catalogue.registerDatasetAsync(DatasetNamespace("yeah"), config).get()
        val datasets = catalogue.getDatasetsAsync().get()

        assertThat(withTimestampRemoved(datasets[coords.namespace]!![coords.id]!!), equalTo(config))
    }

    private fun withTimestampRemoved(actual: DatasetConfig) = actual.copy(metadata = actual.metadata.copy(registered = null))

    companion object {
        @ClassRule
        @JvmField
        val PG = EmbeddedPostgresRules.singleInstance()
    }

    @Rule
    @JvmField
    val RULE = DropwizardAppRule(
        CatalogueApplication::class.java,
        resourceFilePath("catalogue.yml"),
        config("database.port", { PG.embeddedPostgres.port.toString() }),
        config("database.database_name", "postgres")
    )
}
