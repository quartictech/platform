package io.quartic.catalogue.io.quartic.catalogue.datastore;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.util.Maps;
import com.google.cloud.datastore.*;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;

import java.io.IOException;
import java.util.Map;

/**
 * GoogleDatastoreBackend
 *
 * Note that according to https://cloud.google.com/datastore/docs/concepts/structuring_for_strong_consistency
 * strong consistency requires structuring of your data within Google Datastore. In particular only ancestor queries
 * are guaranteed to be strongly consistent. Since we don't want to have to worry about inconsistent state here, we
 * put all of our dataset entities under an ancestor catalogue entity. This should give us strong consistency according
 * to the docs.
 */
public class GoogleDatastoreBackend implements StorageBackend {
    private static final String ANCESTOR = "ancestor";
    private static final String KIND = "dataset";
    private static final String ANCESTOR_KIND = "catalogue";
    private final Datastore datastore;
    private final KeyFactory keyFactory;
    private final Key ancestorKey;
    private final EntitySerDe entitySerDe;

    public GoogleDatastoreBackend(Datastore datastore, String projectId) {
        this.datastore = datastore;
        this.keyFactory = datastore.newKeyFactory().setKind(KIND)
                .setProjectId(projectId)
                .addAncestor(PathElement.of(ANCESTOR_KIND, ANCESTOR))
                .setKind(KIND);
        this.ancestorKey = datastore.newKeyFactory()
                .setProjectId(projectId)
                .setKind(ANCESTOR_KIND)
                .newKey(ANCESTOR);
        this.entitySerDe = new EntitySerDe(keyFactory);
        datastore.put(Entity.newBuilder().setKey(ancestorKey).build());
    }

    public static GoogleDatastoreBackend remote(String projectId, String namespace) {
        Datastore datastore = DatastoreOptions.newBuilder()
                .setNamespace(namespace)
                .setProjectId(projectId)
                .build()
                .getService();

        return new GoogleDatastoreBackend(datastore, projectId);
    }


    @Override
    public DatasetConfig get(DatasetId datasetId) throws IOException {
        Entity entity = datastore.get(keyFactory.newKey(datasetId.getUid()));
        return entitySerDe.entityToDataset(entity);
    }

    @Override
    public void put(DatasetId datasetId, DatasetConfig datasetConfig) throws IOException {
        datastore.runInTransaction(readerWriter -> readerWriter.put(entitySerDe.datasetToEntity(datasetId, datasetConfig)));
    }

    @Override
    public void remove(DatasetId id) throws IOException {
        datastore.delete(keyFactory.newKey(id.getUid()));
    }

    @Override
    public boolean containsKey(DatasetId id) throws IOException {
        return datastore.get(keyFactory.newKey(id.getUid())) != null;
    }

    @Override
    public Map<DatasetId, DatasetConfig> getAll() throws IOException {
        EntityQuery query = Query.newEntityQueryBuilder()
                .setKind(KIND)
                .setFilter(StructuredQuery.PropertyFilter.hasAncestor(ancestorKey))
                .build();

        Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();
        QueryResults<Entity> results = datastore.run(query);
        while (results.hasNext()) {
            Entity entity = results.next();
            datasets.put(new DatasetId(entity.getKey().getName()), entitySerDe.entityToDataset(entity));
        }

        return datasets;
    }

    @Override
    public HealthCheck.Result healthCheck() {
        if(datastore.get(ancestorKey) != null) {
            return HealthCheck.Result.healthy();
        }
        else {
            return HealthCheck.Result.unhealthy("can't find ancestor key");
        }
    }
}
