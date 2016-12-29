package io.quartic.catalogue;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;

import java.io.IOException;
import java.util.Map;

public class InMemoryStorageBackend implements StorageBackend {
    private final Map<DatasetId, DatasetConfig> datasets = Maps.newConcurrentMap();

    @Override
    public DatasetConfig get(DatasetId datasetId) throws IOException {
        return datasets.get(datasetId);
    }

    @Override
    public void put(DatasetId datasetId, DatasetConfig datasetConfig) {
        datasets.put(datasetId, datasetConfig);
    }

    @Override
    public void remove(DatasetId datasetId) {
        datasets.remove(datasetId);
    }

    @Override
    public boolean containsKey(DatasetId id) throws IOException {
        return datasets.containsKey(id);
    }

    @Override
    public Map<DatasetId, DatasetConfig> getAll() throws IOException {
        return ImmutableMap.copyOf(datasets);
    }

    @Override
    public HealthCheck.Result healthCheck() {
        return HealthCheck.Result.healthy(); // can't really be any other way
    }
}
