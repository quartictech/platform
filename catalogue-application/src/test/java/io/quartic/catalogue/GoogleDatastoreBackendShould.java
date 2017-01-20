package io.quartic.catalogue;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.io.quartic.catalogue.datastore.GoogleDatastoreBackend;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GoogleDatastoreBackendShould extends StorageBackendTests {
    private LocalDatastoreHelper helper;
    private GoogleDatastoreBackend backend;

    @Before
    public void setUp() throws IOException, InterruptedException {
        helper = LocalDatastoreHelper.create();
        helper.start();
        backend = new GoogleDatastoreBackend(helper.getOptions()
                .toBuilder()
                .setNamespace("test")
                .build().getService(), helper.getProjectId());
    }

    @After
    public void tearDown() throws InterruptedException, TimeoutException, IOException {
        helper.stop(Duration.millis(3000));
    }

    @Override
    StorageBackend getBackend() {
        return backend;
    }

    @Test
    public void respect_namespace_separation() throws IOException {
        GoogleDatastoreBackend secondBackend = new GoogleDatastoreBackend(helper.getOptions()
                .toBuilder()
                .setNamespace("test2")
                .build().getService(), helper.getProjectId());

        backend.put(DatasetId.fromString("A"), dataset("1"));
        secondBackend.put(DatasetId.fromString("A"), dataset("2"));

        Map<DatasetId, DatasetConfig> datasets = backend.getAll();
        Map<DatasetId, DatasetConfig> secondDatasets = secondBackend.getAll();
        assertThat(datasets.size(), equalTo(1));
        assertThat(datasets.size(), equalTo(1));

        assertThat(datasets.get(DatasetId.fromString("A")).metadata().name(), equalTo("1"));
        assertThat(secondDatasets.get(DatasetId.fromString("A")).metadata().name(), equalTo("2"));
    }
}
