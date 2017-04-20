package io.quartic.catalogue.inmemory;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;

import java.io.IOException;
import java.util.Map;

public class InMemoryStorageBackend implements StorageBackend {
    private final Map<DatasetCoordinates, DatasetConfig> datasets = Maps.newConcurrentMap();

    // TODO
    @Override
    public DatasetConfig get(DatasetCoordinates coords) throws IOException {
        return datasets.get(coords);
    }

    // TODO
    @Override
    public void put(DatasetCoordinates coords, DatasetConfig config) throws IOException {
        datasets.put(coords, config);
    }

    // TODO
    @Override
    public void remove(DatasetCoordinates coords) throws IOException {
        datasets.remove(coords);
    }

    // TODO
    @Override
    public boolean contains(DatasetCoordinates coords) throws IOException {
        return datasets.containsKey(coords);
    }

    // TODO
    @Override
    public Map<DatasetCoordinates, DatasetConfig> getAll() throws IOException {
        return ImmutableMap.copyOf(datasets);
    }

    @Override
    public HealthCheck.Result healthCheck() {
        return HealthCheck.Result.healthy(); // can't really be any other way
    }
}
