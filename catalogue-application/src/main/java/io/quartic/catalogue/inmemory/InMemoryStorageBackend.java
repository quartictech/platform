package io.quartic.catalogue.inmemory;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetNamespace;

import java.io.IOException;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class InMemoryStorageBackend implements StorageBackend {
    private final Map<DatasetId, DatasetConfig> datasets = Maps.newConcurrentMap();

    // TODO
    @Override
    public DatasetConfig get(DatasetCoordinates coords) throws IOException {
        return datasets.get(coords.getId());
    }

    // TODO
    @Override
    public void put(DatasetCoordinates coords, DatasetConfig config) throws IOException {
        datasets.put(coords.getId(), config);
    }

    // TODO
    @Override
    public void remove(DatasetCoordinates coords) throws IOException {
        datasets.remove(coords.getId());
    }

    // TODO
    @Override
    public boolean contains(DatasetCoordinates coords) throws IOException {
        return datasets.containsKey(coords.getId());
    }

    // TODO
    @Override
    public Map<DatasetCoordinates, DatasetConfig> getAll() throws IOException {
        return ((Map<DatasetId, DatasetConfig>) ImmutableMap.copyOf(datasets))
                .entrySet()
                .stream()
                .collect(toMap(
                        e -> new DatasetCoordinates(new DatasetNamespace("foo"), e.getKey()),
                        Map.Entry::getValue
                ));
    }

    @Override
    public HealthCheck.Result healthCheck() {
        return HealthCheck.Result.healthy(); // can't really be any other way
    }
}
