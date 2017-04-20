package io.quartic.catalogue;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public abstract class StorageBackendTests {
    @Test
    public void store_and_retrieve_dataset() throws IOException {
        DatasetConfig datasetConfig = dataset("name");

        getBackend().put(new DatasetId("TEST"), datasetConfig);

        DatasetConfig config = getBackend().get(new DatasetId("TEST"));

        assertThat(config, equalTo(datasetConfig));
    }
    @Test
    public void retrieve_updated_dataset() throws IOException {
        getBackend().put(new DatasetId("TEST"), dataset("Old Name"));
        getBackend().put(new DatasetId("TEST"), dataset("New Name"));

        DatasetConfig config = getBackend().get(new DatasetId("TEST"));

        assertThat(config.getMetadata().getName(), equalTo("New Name"));
    }

    @Test
    public void fetch_all_datasets() throws IOException {
        DatasetConfig datasetA = dataset("A");
        DatasetConfig datasetB = dataset("B");
        DatasetConfig datasetC = dataset("C");
        getBackend().put(new DatasetId("A"), datasetA);
        getBackend().put(new DatasetId("B"), datasetB);
        getBackend().put(new DatasetId("C"), datasetC);

        Map<DatasetId, DatasetConfig> datasets = getBackend().getAll();

        assertThat(datasets.size(), equalTo(3));
        assertThat(datasets.get(new DatasetId("A")), equalTo(datasetA));
        assertThat(datasets.get(new DatasetId("B")), equalTo(datasetB));
        assertThat(datasets.get(new DatasetId("C")), equalTo(datasetC));
    }

    @Test
    public void containskey() throws IOException {
        getBackend().put(new DatasetId("A"), dataset("A"));

        assertThat(getBackend().containsKey(new DatasetId("A")), equalTo(true));
    }

    @Test
    public void remove() throws IOException {
        DatasetId datasetId = new DatasetId("foo");
        DatasetConfig dataset = dataset("foo");
        getBackend().put(datasetId, dataset);

        getBackend().remove(datasetId);

        assertThat(getBackend().getAll().size(), equalTo(0));
    }

    protected DatasetConfig dataset(String name) {
        DatasetMetadata metadata = new DatasetMetadata(
                name,
                "description",
                "attribution",
                Instant.now());
        Map<String, Object> extensions = ImmutableMap.of("A", "B");
        DatasetLocator locator = new CloudGeoJsonDatasetLocator("WAT", false);
        return new DatasetConfig(metadata, locator, extensions);
    }

    abstract StorageBackend getBackend();
}
