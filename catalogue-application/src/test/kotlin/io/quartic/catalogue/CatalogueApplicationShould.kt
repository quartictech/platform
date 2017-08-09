package io.quartic.catalogue

import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.client.client
import io.quartic.common.test.MASTER_KEY_BASE64
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.util.Collections.emptyMap

class CatalogueApplicationShould {

    @Test
    fun retrieve_registered_datasets() {
        val catalogue = client<CatalogueService>(javaClass, "http://localhost:" + RULE.localPort + "/api")

        val config = DatasetConfig(
                DatasetMetadata("Foo", "Bar", "Arlo", null),
                DatasetLocator.PostgresDatasetLocator("a", "b", "c", "d"),
                emptyMap()
        )

        val coords = catalogue.registerDataset(DatasetNamespace("yeah"), config)
        val datasets = catalogue.getDatasets()

        assertThat(withTimestampRemoved(datasets[coords.namespace]!![coords.id]!!), equalTo(config))
    }

    private fun withTimestampRemoved(actual: DatasetConfig) = actual.copy(metadata = actual.metadata.copy(registered = null))

    companion object {
        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule(
            CatalogueApplication::class.java,
            resourceFilePath("catalogue.yml"),
            config("masterKeyBase64", MASTER_KEY_BASE64.veryUnsafe)
        )
    }
}
