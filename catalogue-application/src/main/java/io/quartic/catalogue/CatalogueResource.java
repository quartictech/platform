package io.quartic.catalogue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.common.uid.UidGenerator;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import java.util.Map;

public class CatalogueResource implements CatalogueService {
    private final Map<DatasetId, DatasetConfig> datasets = Maps.newConcurrentMap();
    private final UidGenerator<DatasetId> didGenerator;

    public CatalogueResource(UidGenerator<DatasetId> didGenerator) {
        this.didGenerator = didGenerator;
    }

    public DatasetId registerDataset(DatasetConfig config) {
        // TODO: basic validation
        DatasetId id = didGenerator.get();
        datasets.put(id, config);
        return id;
    }

    public Map<DatasetId, DatasetConfig> getDatasets() {
        return ImmutableMap.copyOf(datasets);
    }

    public DatasetConfig getDataset(@PathParam("id") String id) {
        final DatasetId did = DatasetId.of(id);
        throwIfDatasetNotFound(did);
        return datasets.get(did);
    }

    public void deleteDataset(@PathParam("id") String id) {
        final DatasetId did = DatasetId.of(id);
        throwIfDatasetNotFound(did);
        datasets.remove(did);
    }

    private void throwIfDatasetNotFound(DatasetId id) {
        if (!datasets.containsKey(id)) {
            throw new NotFoundException("No dataset " + id);
        }
    }
}
