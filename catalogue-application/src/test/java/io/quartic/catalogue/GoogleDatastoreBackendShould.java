package io.quartic.catalogue;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.catalogue.io.quartic.catalogue.datastore.GoogleDatastoreBackend;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GoogleDatastoreBackendShould {
    private final LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    @Before
    public void setUp() throws IOException, InterruptedException {
        helper.start();
    }

    @After
    public void tearDown() throws InterruptedException, TimeoutException, IOException {
        helper.stop(Duration.millis(3000));
    }

    @Test
    public void store_and_retrieve_dataset() throws IOException {
        DatasetMetadata metadata = DatasetMetadataImpl.of(
                "name",
                "description",
                "attribution",
                Optional.empty());
        Map<String, Object> extensions = ImmutableMap.of("A", "B");
        DatasetLocator locator = CloudGeoJsonDatasetLocatorImpl.of("WAT");
        DatasetConfig datasetConfig = DatasetConfigImpl.of(
                metadata,
                locator,
                extensions);

        GoogleDatastoreBackend backend = new GoogleDatastoreBackend(helper.getOptions()
                .toBuilder().build().getService());
        backend.put(DatasetId.fromString("TEST"), datasetConfig);

        DatasetConfig config = backend.get(DatasetId.fromString("TEST"));

        assertThat(config.metadata(), equalTo(metadata));
        assertThat(config.locator(), equalTo(locator));
        assertThat(config.extensions(), equalTo(extensions));
    }
}
