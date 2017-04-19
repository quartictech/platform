package io.quartic.catalogue;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.model.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.catalogue.api.model.DatasetNamespace;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public abstract class StorageBackendTests {
    // TODO: validate namespace isolation

    @Test
    public void store_and_retrieve_dataset() throws IOException {
        DatasetConfig datasetConfig = dataset("name");

        getBackend().put(coordsFrom(new DatasetId("TEST")), datasetConfig);

        DatasetConfig config = getBackend().get(coordsFrom(new DatasetId("TEST")));

        assertThat(config, equalTo(datasetConfig));
    }
    @Test
    public void retrieve_updated_dataset() throws IOException {
        getBackend().put(coordsFrom(new DatasetId("TEST")), dataset("Old Name"));
        getBackend().put(coordsFrom(new DatasetId("TEST")), dataset("New Name"));

        DatasetConfig config = getBackend().get(coordsFrom(new DatasetId("TEST")));

        assertThat(config.getMetadata().getName(), equalTo("New Name"));
    }

    @Test
    public void fetch_all_datasets() throws IOException {
        DatasetConfig datasetA = dataset("A");
        DatasetConfig datasetB = dataset("B");
        DatasetConfig datasetC = dataset("C");
        getBackend().put(coordsFrom(new DatasetId("A")), datasetA);
        getBackend().put(coordsFrom(new DatasetId("B")), datasetB);
        getBackend().put(coordsFrom(new DatasetId("C")), datasetC);

        Map<DatasetCoordinates, DatasetConfig> datasets = getBackend().getAll();

        assertThat(datasets.size(), equalTo(3));
        assertThat(datasets.get(coordsFrom(new DatasetId("A"))), equalTo(datasetA));
        assertThat(datasets.get(coordsFrom(new DatasetId("B"))), equalTo(datasetB));
        assertThat(datasets.get(coordsFrom(new DatasetId("C"))), equalTo(datasetC));
    }

    @Test
    public void contains() throws IOException {
        getBackend().put(coordsFrom(new DatasetId("A")), dataset("A"));

        assertThat(getBackend().contains(coordsFrom(new DatasetId("A"))), equalTo(true));
    }

    @Test
    public void remove() throws IOException {
        DatasetId datasetId = new DatasetId("foo");
        DatasetConfig dataset = dataset("foo");
        getBackend().put(coordsFrom(datasetId), dataset);

        getBackend().remove(coordsFrom(datasetId));

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

    protected abstract StorageBackend getBackend();

    protected DatasetCoordinates coordsFrom(DatasetId id) {
        return new DatasetCoordinates(new DatasetNamespace("foo"), id);
    }
}
