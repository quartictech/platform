package io.quartic.catalogue;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.catalogue.api.model.DatasetMetadata;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public abstract class StorageBackendTests {
    @Test
    public void store_and_retrieve_dataset() throws IOException {
        final DatasetConfig datasetConfig = dataset("name");
        final DatasetCoordinates coords = coords("namespace", "abc");

        getBackend().put(coords, datasetConfig);

        assertThat(getBackend().get(coords), equalTo(datasetConfig));
    }

    @Test
    public void retrieve_updated_dataset() throws IOException {
        final DatasetCoordinates coords = coords("namespace", "abc");

        getBackend().put(coords, dataset("Old Name"));
        getBackend().put(coords, dataset("New Name"));

        assertThat(getBackend().get(coords).getMetadata().getName(), equalTo("New Name"));
    }

    @Test
    public void fetch_all_datasets() throws IOException {
        DatasetConfig datasetA = dataset("A");
        DatasetConfig datasetB = dataset("B");
        DatasetConfig datasetC = dataset("C");
        getBackend().put(coords("foo", "A"), datasetA);
        getBackend().put(coords("foo", "B"), datasetB);
        getBackend().put(coords("bar", "C"), datasetC);

        assertThat(getBackend().getAll(), equalTo(map(
                entry(coords("foo", "A"), datasetA),
                entry(coords("foo", "B"), datasetB),
                entry(coords("bar", "C"), datasetC)
        )));
    }

    @Test
    public void enforce_dataset_namespace_isolation() throws IOException {
        final DatasetCoordinates coordsA = coords("ns1", "abc");
        final DatasetCoordinates coordsB = coords("ns21", "abc");
        final DatasetConfig datasetA = dataset("foo");
        final DatasetConfig datasetB = dataset("bar");

        getBackend().put(coordsA, datasetA);
        getBackend().put(coordsB, datasetB);

        assertThat(getBackend().get(coordsA), equalTo(datasetA));
        assertThat(getBackend().get(coordsB), equalTo(datasetB));
    }

    @Test
    public void contains() throws IOException {
        final DatasetCoordinates coords = coords("namespace", "abc");

        getBackend().put(coords, dataset("A"));

        assertThat(getBackend().contains(coords), equalTo(true));
    }

    @Test
    public void remove() throws IOException {
        final DatasetConfig dataset = dataset("foo");
        final DatasetCoordinates coords = coords("namespace", "abc");

        getBackend().put(coords, dataset);
        getBackend().remove(coords);

        assertThat(getBackend().getAll(), equalTo(emptyMap()));
    }

    protected DatasetConfig dataset(String name) {
        DatasetMetadata metadata = new DatasetMetadata(
                name,
                "description",
                "attribution",
                Instant.now());
        Map<String, Object> extensions = ImmutableMap.of("A", "B");
        DatasetLocator locator = new DatasetLocator.CloudGeoJsonDatasetLocator("WAT", false);
        return new DatasetConfig(metadata, locator, extensions);
    }

    protected abstract StorageBackend getBackend();

    protected DatasetCoordinates coords(String namespace, String id) {
        return new DatasetCoordinates(namespace, id);
    }
}
