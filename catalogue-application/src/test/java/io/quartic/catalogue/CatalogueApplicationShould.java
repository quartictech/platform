package io.quartic.catalogue;

import io.dropwizard.testing.junit.DropwizardAppRule;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.catalogue.api.PostgresDatasetLocator;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.quartic.common.client.ClientUtilsKt.client;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class CatalogueApplicationShould {
    @ClassRule
    public static final DropwizardAppRule<CatalogueConfiguration> RULE =
            new DropwizardAppRule<>(CatalogueApplication.class, resourceFilePath("catalogue.yml"));

    @Test
    public void retrieve_registered_datasets() throws Exception {
        final CatalogueService catalogue = client(CatalogueService.class, getClass(), "http://localhost:" + RULE.getLocalPort() + "/api");

        final DatasetConfig config = new DatasetConfig(
                new DatasetMetadata("Foo", "Bar", "Arlo", null),
                new PostgresDatasetLocator("a", "b", "c", "d"),
                emptyMap()
        );

        DatasetId did = catalogue.registerDataset(config);
        final Map<DatasetId, DatasetConfig> datasets = catalogue.getDatasets();

        assertThat(withTimestampRemoved(datasets.get(did)), equalTo(config));
    }

    private DatasetConfig withTimestampRemoved(DatasetConfig actual) {
        return new DatasetConfig(
                new DatasetMetadata(
                        actual.getMetadata().getName(),
                        actual.getMetadata().getDescription(),
                        actual.getMetadata().getAttribution(),
                        null
                ),
                actual.getLocator(),
                actual.getExtensions());  // TODO - use .copy() once in Kotlin
    }
}
