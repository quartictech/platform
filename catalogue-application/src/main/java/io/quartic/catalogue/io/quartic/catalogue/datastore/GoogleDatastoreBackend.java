package io.quartic.catalogue.io.quartic.catalogue.datastore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.util.Maps;
import com.google.cloud.datastore.*;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;

public class GoogleDatastoreBackend implements StorageBackend {
    private final Datastore datastore;

    public GoogleDatastoreBackend(Datastore datastore) {
        this.datastore = datastore;
    }

    public static GoogleDatastoreBackend remote(String projectId) {
        return new GoogleDatastoreBackend(DatastoreOptions.newBuilder()
                .setProjectId(projectId)
               .build()
               .getService());
    }

    private DatasetConfig entityToDataset(Entity entity) throws IOException {
        DatasetMetadata metadata = DatasetMetadataImpl.of(
                entity.getString("name"),
                entity.getString("description"),
                entity.getString("attribution"),
                Optional.ofNullable(entity.getString("icon")).map(IconImpl::of)
        );

        DatasetLocator locator = OBJECT_MAPPER.readValue(entity.getBlob("locator").asInputStream(),
                DatasetLocator.class);

        Map<String, Object> extensions = OBJECT_MAPPER.readValue(entity.getBlob("extensions").asInputStream(),
                new TypeReference<Map<String, Object>>() { });

        return DatasetConfigImpl.of(metadata, locator, extensions);
    }

    private Key idToKey(DatasetId id) {
       return datastore.newKeyFactory()
               .setKind("dataset")
               .newKey(id.uid());
    }

    @Override
    public DatasetConfig get(DatasetId datasetId) throws IOException {
        Entity entity = datastore.get(idToKey(datasetId));
        return entityToDataset(entity);
    }

    @Override
    public void put(DatasetId datasetId, DatasetConfig datasetConfig) throws IOException {
        Entity.Builder entityBuilder = Entity.newBuilder(idToKey(datasetId));

        entityBuilder.set("name", datasetConfig.metadata().name());
        entityBuilder.set("description", datasetConfig.metadata().description());
        entityBuilder.set("attribution", datasetConfig.metadata().attribution());
        if (datasetConfig.metadata().icon().isPresent()) {
            entityBuilder.set("icon", datasetConfig.metadata().icon().map(Icon::icon).get());
        }
        else {
            entityBuilder.setNull("icon");
        }

        entityBuilder.set("locator", Blob.copyFrom(OBJECT_MAPPER.writeValueAsBytes(datasetConfig.locator())));
        entityBuilder.set("extensions", Blob.copyFrom(OBJECT_MAPPER.writeValueAsBytes(datasetConfig.extensions())));

        datastore.put(entityBuilder.build());
    }

    @Override
    public void remove(DatasetId id) throws IOException {
        datastore.delete(datastore.newKeyFactory().newKey(id.uid()));
    }

    @Override
    public boolean containsKey(DatasetId id) throws IOException {
        return datastore.get(datastore.newKeyFactory().newKey(id.uid())) != null;
    }

    @Override
    public Map<DatasetId, DatasetConfig> getAll() throws IOException {
        EntityQuery query = Query.newEntityQueryBuilder()
                .setKind("dataset")
                .build();

        Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();
        QueryResults<Entity> results = datastore.run(query);
        while (results.hasNext()) {
            Entity entity = results.next();
            datasets.put(DatasetId.fromString(entity.getKey().getName()), entityToDataset(entity));
        }

        return datasets;
    }
}
