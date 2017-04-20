package io.quartic.catalogue.datastore;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.util.Maps;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;

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
    private static final String ANCESTOR_KIND = "catalogue";
    private static final String NAMESPACE_KIND = "namespace";
    private static final String DATASET_KIND = "dataset";
    private final Datastore datastore;
    private final Key ancestorKey;
    private final EntitySerDe entitySerDe;

    public GoogleDatastoreBackend(Datastore datastore) {
        this.datastore = datastore;
        this.ancestorKey = datastore.newKeyFactory()
                .setKind(ANCESTOR_KIND)
                .newKey(ANCESTOR);
        this.entitySerDe = new EntitySerDe(this::key);
        datastore.put(Entity.newBuilder().setKey(ancestorKey).build());
    }

    public static GoogleDatastoreBackend remote(String projectId, String namespace) {
        Datastore datastore = DatastoreOptions.newBuilder()
                .setNamespace(namespace)
                .setProjectId(projectId)
                .build()
                .getService();

        return new GoogleDatastoreBackend(datastore);
    }

    @Override
    public DatasetConfig get(DatasetCoordinates coords) throws IOException {
        Entity entity = datastore.get(key(coords));
        return entitySerDe.entityToDataset(entity);
    }

    @Override
    public void put(DatasetCoordinates coords, DatasetConfig config) throws IOException {
        datastore.runInTransaction(readerWriter -> readerWriter.put(entitySerDe.datasetToEntity(coords, config)));
    }

    @Override
    public void remove(DatasetCoordinates coords) throws IOException {
        datastore.delete(key(coords));
    }

    @Override
    public boolean contains(DatasetCoordinates coords) throws IOException {
        return datastore.get(key(coords)) != null;
    }

    @Override
    public Map<DatasetCoordinates, DatasetConfig> getAll() throws IOException {
        EntityQuery query = Query.newEntityQueryBuilder()
                .setKind(DATASET_KIND)
                .setFilter(StructuredQuery.PropertyFilter.hasAncestor(ancestorKey))
                .build();

        Map<DatasetCoordinates, DatasetConfig> datasets = Maps.newHashMap();
        QueryResults<Entity> results = datastore.run(query);
        while (results.hasNext()) {
            Entity entity = results.next();
            datasets.put(
                    new DatasetCoordinates(entity.getKey().getParent().getName(), entity.getKey().getName()),
                    entitySerDe.entityToDataset(entity)
            );
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

    private Key key(DatasetCoordinates coords) {
        return datastore.newKeyFactory()
                .addAncestors(
                        PathElement.of(ANCESTOR_KIND, ANCESTOR),
                        PathElement.of(NAMESPACE_KIND, coords.getNamespace().getNamespace())
                )
                .setKind(DATASET_KIND)
                .newKey(coords.getId().getUid());
    }
}
