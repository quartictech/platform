package io.quartic.catalogue;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import io.quartic.catalogue.io.quartic.catalogue.datastore.GoogleDatastoreBackend;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class GoogleDatastoreBackendShould extends StorageBackendTests {
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

    @Override
    StorageBackend getBackend() {
        return backend;
    }
}
