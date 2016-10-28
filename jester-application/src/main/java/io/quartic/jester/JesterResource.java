package io.quartic.jester;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.quartic.jester.api.DatasetId;
import io.quartic.jester.api.DatasetMetadata;
import io.quartic.jester.api.JesterService;
import io.quartic.weyl.common.uid.UidGenerator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Map;

public class JesterResource implements JesterService {
    private final Map<DatasetId, DatasetMetadata> datasets = Maps.newConcurrentMap();
    private final UidGenerator<DatasetId> didGenerator;

    public JesterResource(UidGenerator<DatasetId> didGenerator) {
        this.didGenerator = didGenerator;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<DatasetId> listDatasets() {
        return ImmutableList.copyOf(datasets.keySet());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DatasetId registerDataset(DatasetMetadata metadata) {
        // TODO: basic validation
        DatasetId id = didGenerator.get();
        datasets.put(id, metadata);
        return id;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DatasetMetadata getDataset(@PathParam("id") String id) {
        final DatasetId did = DatasetId.of(id);
        throwIfDatasetNotFound(did);
        return datasets.get(did);
    }

    @DELETE
    @Path("/{id}")
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
