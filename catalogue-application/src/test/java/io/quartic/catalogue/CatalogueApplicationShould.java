package io.quartic.catalogue;

import io.dropwizard.testing.junit.DropwizardAppRule;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.ClientBuilder;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class CatalogueApplicationShould {
    @ClassRule
    public static final DropwizardAppRule<CatalogueConfiguration> RULE =
            new DropwizardAppRule<>(CatalogueApplication.class, resourceFilePath("catalogue.yml"));

    @Test
    public void retrieve_registered_datasets() throws Exception {
        final CatalogueService catalogue = ClientBuilder.build(CatalogueService.class, getClass(), "http://localhost:" + RULE.getLocalPort() + "/api");

        final DatasetConfig config = DatasetConfigImpl.of(
                DatasetMetadataImpl.of("Foo", "Bar", "Arlo", Optional.empty(), Optional.empty()),
                PostgresDatasetLocatorImpl.of("a", "b", "c", "d"),
                emptyMap()
        );

        DatasetId did = catalogue.registerDataset(config);
        final Map<DatasetId, DatasetConfig> datasets = catalogue.getDatasets();

        assertThat(withTimestampRemoved(datasets.get(did)), equalTo(config));
    }

    private DatasetConfig withTimestampRemoved(DatasetConfig actual) {
        return DatasetConfigImpl.copyOf(actual)
                .withMetadata(DatasetMetadataImpl.copyOf(actual.metadata()).withRegistered(Optional.empty()));
    }
}
