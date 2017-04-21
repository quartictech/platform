package io.quartic.catalogue.datastore;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.StorageBackendTests;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

public class GoogleDatastoreBackendShould extends StorageBackendTests {
    private static LocalDatastoreHelper helper;
    private Datastore datastore;
    private GoogleDatastoreBackend backend;

    @BeforeClass
    public static void setupEmulator() throws IOException, InterruptedException {
        helper = LocalDatastoreHelper.create();
        helper.start();
    }

    @AfterClass
    public static void tearDownEmulator() throws InterruptedException, TimeoutException, IOException {
        helper.stop(Duration.millis(3000));
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        helper.reset();
        datastore = helper.getOptions()
                .toBuilder()
                .setNamespace("test")
                .build().getService();
        backend = new GoogleDatastoreBackend(datastore);
    }

    @Override
    protected StorageBackend getBackend() {
        return backend;
    }

    @Test
    public void respect_datastore_namespace_separation() throws IOException {
        GoogleDatastoreBackend secondBackend = new GoogleDatastoreBackend(helper.getOptions()
                .toBuilder()
                .setNamespace("test2")
                .build().getService());

        backend.put(coords("namespace", "A"), dataset("1"));
        secondBackend.put(coords("namespace", "A"), dataset("2"));

        Map<DatasetCoordinates, DatasetConfig> datasets = backend.getAll();
        Map<DatasetCoordinates, DatasetConfig> secondDatasets = secondBackend.getAll();

        assertThat(datasets.size(), equalTo(1));
        assertThat(secondDatasets.size(), equalTo(1));

        assertThat(datasets.get(coords("namespace", "A")).getMetadata().getName(), equalTo("1"));
        assertThat(secondDatasets.get(coords("namespace", "A")).getMetadata().getName(), equalTo("2"));
    }

    @Test
    public void ignore_unnamespaced_datasets() throws Exception {
        // This is where a v1 dataset would be
        final Key key = datastore.newKeyFactory()
                .addAncestors(PathElement.of("catalogue", "ancestor"))
                .setKind("dataset")
                .newKey("foobles");
        datastore.put(Entity.newBuilder(key).build());

        assertThat(backend.getAll().entrySet(), empty());
    }
}