package io.quartic.catalogue.io.quartic.catalogue.datastore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.util.Maps;
import com.google.cloud.datastore.*;
import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.api.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;

public class GoogleDatastoreBackend implements StorageBackend {
    private static final String ANCESTOR = "ancestor";
    private static final String KIND = "dataset";
    private static final String ANCESTOR_KIND = "catalogue";
    private final Datastore datastore;
    private final KeyFactory keyFactory;
    private final Key ancestorKey;

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
        datastore.put(Entity.newBuilder().setKey(ancestorKey).build());
    }

    public static GoogleDatastoreBackend remote(String projectId) {
        Datastore datastore = DatastoreOptions.newBuilder()
                .setProjectId(projectId)
               .build()
               .getService();

        return new GoogleDatastoreBackend(datastore, projectId);
    }

    private DatasetConfig entityToDataset(Entity entity) throws IOException {
        DatasetMetadata metadata = DatasetMetadataImpl.of(
                entity.getString("name"),
                entity.getString("description"),
                entity.getString("attribution"),
                Optional.ofNullable(entity.getDateTime("registered"))
                        .map(dt -> Instant.ofEpochMilli(dt.getTimestampMillis())),
                Optional.ofNullable(entity.getString("icon")).map(IconImpl::of)
        );

        DatasetLocator locator = OBJECT_MAPPER.readValue(entity.getBlob("locator").asInputStream(),
                DatasetLocator.class);

        Map<String, Object> extensions = OBJECT_MAPPER.readValue(entity.getBlob("extensions").asInputStream(),
                new TypeReference<Map<String, Object>>() { });

        return DatasetConfigImpl.of(metadata, locator, extensions);
    }

    private Entity datasetToEntity(DatasetId datasetId, DatasetConfig datasetConfig) throws JsonProcessingException {
        Entity.Builder entityBuilder = Entity.newBuilder(idToKey(datasetId));

        entityBuilder.set("name", datasetConfig.metadata().name());
        entityBuilder.set("description", datasetConfig.metadata().description());
        entityBuilder.set("attribution", datasetConfig.metadata().attribution());
        if (datasetConfig.metadata().registered().isPresent()) {
            entityBuilder.set("registered",
                    DateTime.copyFrom(new Date(datasetConfig.metadata().registered().get().toEpochMilli())));
        }
        else {
            entityBuilder.setNull("registered");
        }
        if (datasetConfig.metadata().icon().isPresent()) {
            entityBuilder.set("icon", datasetConfig.metadata().icon().map(Icon::icon).get());
        } else {
            entityBuilder.setNull("icon");
        }

        entityBuilder.set("locator", Blob.copyFrom(OBJECT_MAPPER.writeValueAsBytes(datasetConfig.locator())));
        entityBuilder.set("extensions", Blob.copyFrom(OBJECT_MAPPER.writeValueAsBytes(datasetConfig.extensions())));
        return entityBuilder.build();
    }

    private Key idToKey(DatasetId id) {
        return keyFactory.newKey(id.uid());
    }

    @Override
    public DatasetConfig get(DatasetId datasetId) throws IOException {
        Entity entity = datastore.get(idToKey(datasetId));
        return entityToDataset(entity);
    }

    @Override
    public void put(DatasetId datasetId, DatasetConfig datasetConfig) throws IOException {
        datastore.runInTransaction(readerWriter -> readerWriter.put(datasetToEntity(datasetId, datasetConfig)));
    }

    @Override
    public void remove(DatasetId id) throws IOException {
        datastore.delete(datastore.newKeyFactory().newKey(id.uid()));
    }

    @Override
    public boolean containsKey(DatasetId id) throws IOException {
        return datastore.get(keyFactory.newKey(id.uid())) != null;
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
            datasets.put(DatasetId.fromString(entity.getKey().getName()), entityToDataset(entity));
        }

        return datasets;
    }
}
