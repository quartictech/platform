package io.quartic.catalogue;

import com.codahale.metrics.health.HealthCheck;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetCoordinates;
import io.quartic.catalogue.api.DatasetId;

import java.io.IOException;
import java.util.Map;

public interface StorageBackend {
    DatasetConfig get(DatasetCoordinates datasetCoords) throws IOException;
    void put(DatasetCoordinates datasetCoords, DatasetConfig datasetConfig) throws IOException;
    void remove(DatasetCoordinates datasetCoords) throws IOException;
    boolean contains(DatasetCoordinates datasetCoords) throws IOException;
    Map<DatasetCoordinates, DatasetConfig> getAllAgainstCoords() throws IOException;    // TODO: rename method to getAll()

    // TODO - cull all of these
    DatasetConfig get(DatasetId datasetId) throws IOException;
    void put(DatasetId datasetId, DatasetConfig datasetConfig) throws IOException;
    void remove(DatasetId id) throws IOException;
    boolean containsKey(DatasetId id) throws IOException;
    Map<DatasetId, DatasetConfig> getAll() throws IOException;


    HealthCheck.Result healthCheck();
}
