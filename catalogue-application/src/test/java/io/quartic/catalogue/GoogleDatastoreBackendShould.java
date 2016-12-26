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
    private LocalDatastoreHelper helper;
    private GoogleDatastoreBackend backend;

    @Before
    public void setUp() throws IOException, InterruptedException {
        helper = LocalDatastoreHelper.create();
        helper.start();
        backend = new GoogleDatastoreBackend(helper.getOptions()
                .toBuilder().build().getService(), helper.getProjectId());
    }

    @After
    public void tearDown() throws InterruptedException, TimeoutException, IOException {
        helper.stop(Duration.millis(3000));
    }

    @Test
    public void store_and_retrieve_dataset() throws IOException {
        DatasetConfig datasetConfig = dataset("name");

        backend.put(DatasetId.fromString("TEST"), datasetConfig);

        DatasetConfig config = backend.get(DatasetId.fromString("TEST"));

        assertThat(config.metadata(), equalTo(datasetConfig.metadata()));
        assertThat(config.locator(), equalTo(datasetConfig.locator()));
        assertThat(config.extensions(), equalTo(datasetConfig.extensions()));
    }

    @Test
    public void fetch_all_datasets() throws IOException {
        backend.put(DatasetId.fromString("A"), dataset("A"));
        backend.put(DatasetId.fromString("B"), dataset("B"));
        backend.put(DatasetId.fromString("C"), dataset("C"));

        Map<DatasetId, DatasetConfig> datasets = backend.getAll();

        assertThat(datasets.size(), equalTo(3));
        assertThat(datasets.get(DatasetId.fromString("A")), equalTo(dataset("A")));
        assertThat(datasets.get(DatasetId.fromString("B")), equalTo(dataset("B")));
        assertThat(datasets.get(DatasetId.fromString("C")), equalTo(dataset("C")));
    }

    private DatasetConfig dataset(String name) {
        DatasetMetadata metadata = DatasetMetadataImpl.of(
                name,
                "description",
                "attribution",
                Optional.empty(),
                Optional.empty());
        Map<String, Object> extensions = ImmutableMap.of("A", "B");
        DatasetLocator locator = CloudGeoJsonDatasetLocatorImpl.of("WAT");
        return DatasetConfigImpl.of(
                metadata,
                locator,
                extensions);
    }
}
