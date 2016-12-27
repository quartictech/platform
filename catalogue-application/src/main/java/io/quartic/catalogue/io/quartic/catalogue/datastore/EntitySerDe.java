package io.quartic.catalogue.io.quartic.catalogue.datastore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.datastore.*;
import io.quartic.catalogue.api.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;

public class EntitySerDe {
    private static final String VERSION = "version";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ATTRIBUTION = "attribution";
    private static final String REGISTERED = "registered";
    private static final String ICON = "icon";
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
        DatasetMetadata metadata = DatasetMetadataImpl.of(
                entity.getString(NAME),
                entity.getString(DESCRIPTION),
                entity.getString(ATTRIBUTION),
                Optional.ofNullable(entity.getDateTime(REGISTERED))
                        .map(dt -> Instant.ofEpochMilli(dt.getTimestampMillis())),
                Optional.ofNullable(entity.getString(ICON)).map(IconImpl::of)
        );

        DatasetLocator locator = OBJECT_MAPPER.readValue(entity.getBlob(LOCATOR).asInputStream(),
                DatasetLocator.class);

        Map<String, Object> extensions = OBJECT_MAPPER.readValue(entity.getBlob(EXTENSIONS).asInputStream(),
                new TypeReference<Map<String, Object>>() { });

        return DatasetConfigImpl.of(metadata, locator, extensions);
    }

    private void checkVersion(Long version) throws IOException {
        if (! version.equals(CURRENT_VERSION)) {
            String errorMessage = String.format(
                    "version mismatch: database has %d, code has %d. time to write some migrations!",
                    version,
                    CURRENT_VERSION);
            throw new IOException(errorMessage);
        }
    }

    public Entity datasetToEntity(DatasetId datasetId, DatasetConfig datasetConfig) throws JsonProcessingException {
        Entity.Builder entityBuilder = Entity.newBuilder(keyFactory.newKey(datasetId.uid()));

        entityBuilder.set(VERSION, CURRENT_VERSION);
        entityBuilder.set(NAME, datasetConfig.metadata().name());
        entityBuilder.set(DESCRIPTION, datasetConfig.metadata().description());
        entityBuilder.set(ATTRIBUTION, datasetConfig.metadata().attribution());
        if (datasetConfig.metadata().registered().isPresent()) {
            entityBuilder.set(REGISTERED,
                    DateTime.copyFrom(new Date(datasetConfig.metadata().registered().get().toEpochMilli())));
        }
        else {
            entityBuilder.setNull(REGISTERED);
        }
        if (datasetConfig.metadata().icon().isPresent()) {
            entityBuilder.set(ICON, datasetConfig.metadata().icon().map(Icon::icon).get());
        } else {
            entityBuilder.setNull(ICON);
        }

        entityBuilder.set(LOCATOR, Blob.copyFrom(OBJECT_MAPPER.writeValueAsBytes(datasetConfig.locator())));
        entityBuilder.set(EXTENSIONS, Blob.copyFrom(OBJECT_MAPPER.writeValueAsBytes(datasetConfig.extensions())));
        return entityBuilder.build();
    }
}
