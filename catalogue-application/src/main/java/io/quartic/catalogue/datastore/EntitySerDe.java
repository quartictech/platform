package io.quartic.catalogue.datastore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.DateTime;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;

public class EntitySerDe {
    private static final String VERSION = "version";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ATTRIBUTION = "attribution";
    private static final String REGISTERED = "registered";
    private static final String LOCATOR = "locator";
    private static final String EXTENSIONS = "extensions";
    private static final Long CURRENT_VERSION = 1L;
    private final KeyFactory keyFactory;

    public EntitySerDe(KeyFactory keyFactory) {
        this.keyFactory = keyFactory;
    }

    public DatasetConfig entityToDataset(Entity entity) throws IOException {
        Long version = entity.getLong(VERSION);
        checkVersion(version);
        DatasetMetadata metadata = new DatasetMetadata(
                entity.getString(NAME),
                entity.getString(DESCRIPTION),
                entity.getString(ATTRIBUTION),
                Optional.ofNullable(entity.getDateTime(REGISTERED))
                        .map(dt -> Instant.ofEpochMilli(dt.getTimestampMillis())).orElse(null)
        );

        DatasetLocator locator = objectMapper().readValue(entity.getBlob(LOCATOR).asInputStream(),
                DatasetLocator.class);

        Map<String, Object> extensions = objectMapper().readValue(entity.getBlob(EXTENSIONS).asInputStream(),
                new TypeReference<Map<String, Object>>() { });

        return new DatasetConfig(metadata, locator, extensions);
    }

    private void checkVersion(Long version) throws IOException {
        if (!version.equals(CURRENT_VERSION)) {
            String errorMessage = String.format(
                    "version mismatch: database has %d, code has %d. time to write some migrations!",
                    version,
                    CURRENT_VERSION);
            throw new IOException(errorMessage);
        }
    }

    public Entity datasetToEntity(DatasetId datasetId, DatasetConfig datasetConfig) throws JsonProcessingException {
        Entity.Builder entityBuilder = Entity.newBuilder(keyFactory.newKey(datasetId.getUid()));

        entityBuilder.set(VERSION, CURRENT_VERSION);
        entityBuilder.set(NAME, datasetConfig.getMetadata().getName());
        entityBuilder.set(DESCRIPTION, datasetConfig.getMetadata().getDescription());
        entityBuilder.set(ATTRIBUTION, datasetConfig.getMetadata().getAttribution());
        if (datasetConfig.getMetadata().getRegistered() != null) {
            entityBuilder.set(REGISTERED,
                    DateTime.copyFrom(new Date(datasetConfig.getMetadata().getRegistered().toEpochMilli())));
        }
        else {
            entityBuilder.setNull(REGISTERED);
        }

        entityBuilder.set(LOCATOR, Blob.copyFrom(objectMapper().writeValueAsBytes(datasetConfig.getLocator())));
        entityBuilder.set(EXTENSIONS, Blob.copyFrom(objectMapper().writeValueAsBytes(datasetConfig.getExtensions())));
        return entityBuilder.build();
    }
}
