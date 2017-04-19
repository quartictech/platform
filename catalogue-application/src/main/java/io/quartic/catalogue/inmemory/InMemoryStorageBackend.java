package io.quartic.catalogue.inmemory;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetCoordinates;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetNamespace;

import java.io.IOException;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class InMemoryStorageBackend implements StorageBackend {
    private final Map<DatasetId, DatasetConfig> datasets = Maps.newConcurrentMap();

    // TODO
    @Override
    public DatasetConfig get(DatasetCoordinates datasetCoords) throws IOException {
        return get(datasetCoords.getId());
    }

    // TODO
    @Override
    public void put(DatasetCoordinates datasetCoords, DatasetConfig datasetConfig) throws IOException {
        put(datasetCoords.getId(), datasetConfig);
    }

    // TODO
    @Override
    public void remove(DatasetCoordinates datasetCoords) throws IOException {
        remove(datasetCoords.getId());
    }

    // TODO
    @Override
    public boolean contains(DatasetCoordinates datasetCoords) throws IOException {
        return containsKey(datasetCoords.getId());
    }

    // TODO
    @Override
    public Map<DatasetCoordinates, DatasetConfig> getAllAgainstCoords() throws IOException {
        return getAll()
                .entrySet()
                .stream()
                .collect(toMap(
                        e -> new DatasetCoordinates(new DatasetNamespace("foo"), e.getKey()),
                        Map.Entry::getValue
                ));
    }

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
