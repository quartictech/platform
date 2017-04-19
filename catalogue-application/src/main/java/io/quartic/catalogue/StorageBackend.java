package io.quartic.catalogue;

import com.codahale.metrics.health.HealthCheck;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetCoordinates;

import java.io.IOException;
import java.util.Map;

public interface StorageBackend {
    DatasetConfig get(DatasetCoordinates coords) throws IOException;
    void put(DatasetCoordinates coords, DatasetConfig config) throws IOException;
    void remove(DatasetCoordinates coords) throws IOException;
    boolean contains(DatasetCoordinates coords) throws IOException;
    Map<DatasetCoordinates, DatasetConfig> getAll() throws IOException;
    HealthCheck.Result healthCheck();
}
