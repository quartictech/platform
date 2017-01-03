package io.quartic.catalogue.api;

import org.junit.Test;

import java.io.IOException;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public abstract class DatasetLocatorTests<T extends DatasetLocator> {
    protected abstract T locator();
    protected abstract String json();

    @Test
    public void deserialize_as_expected() throws IOException {
        T datasetLocator = objectMapper().readValue(json(), (Class<T>) DatasetLocator.class);
        assertThat(datasetLocator, equalTo(locator()));
    }
}
