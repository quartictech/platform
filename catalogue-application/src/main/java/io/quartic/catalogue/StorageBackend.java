package io.quartic.catalogue;

import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;

import java.io.IOException;
import java.util.Map;

public interface StorageBackend {
    DatasetConfig get(DatasetId datasetId) throws IOException;
    void put(DatasetId datasetId, DatasetConfig datasetConfig) throws IOException;
    void remove(DatasetId id) throws IOException;
    boolean containsKey(DatasetId id) throws IOException;
    Map<DatasetId, DatasetConfig> getAll() throws IOException;
}
