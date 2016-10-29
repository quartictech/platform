package io.quartic.jester;

import io.dropwizard.testing.junit.DropwizardAppRule;
import io.quartic.common.client.ClientBuilder;
import io.quartic.jester.api.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class JesterApplicationShould {
    @ClassRule
    public static final DropwizardAppRule<JesterConfiguration> RULE =
            new DropwizardAppRule<>(JesterApplication.class, resourceFilePath("jester.yml"));
    private final JesterService jester = ClientBuilder.build(JesterService.class, "http://localhost:8090/api");

    @Test
    public void retrieve_registered_datasets() throws Exception {
        final DatasetConfig config = DatasetConfig.of(
                DatasetMetadata.of("Foo", "Bar", "Arlo", Optional.empty()),
                PostgresDatasetSource.of("a", "b", "c", "d")
        );

        DatasetId did = jester.registerDataset(config);
        final Map<DatasetId, DatasetConfig> datasets = jester.getDatasets();

        assertThat(datasets.get(did), equalTo(config));
    }
}
