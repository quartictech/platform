package io.quartic.catalogue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.*;
import io.quartic.catalogue.io.quartic.catalogue.datastore.EntitySerDe;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class EntitySerDeShould {
    KeyFactory keyFactory = new KeyFactory("test")
            .setKind("dataset");
    EntitySerDe entitySerDe = new EntitySerDe(keyFactory);

    @Test
    public void serialize_deserialize_correctly() throws IOException {
        DatasetConfig datasetConfig = DatasetConfigImpl.of(
                DatasetMetadataImpl.of(
                        "name",
                        "description",
                        "attribution",
                        Optional.of(Instant.now()),
                        Optional.of(IconImpl.of("icon"))
                ),
                GeoJsonDatasetLocatorImpl.of("wat"),
                ImmutableMap.of("foo", "bar", "wat", ImmutableMap.of("ladispute", 1337))
        );

        Entity entity = entitySerDe.datasetToEntity(DatasetId.fromString("sweet"), datasetConfig);
        DatasetConfig datasetConfig1 = entitySerDe.entityToDataset(entity);

        assertThat(datasetConfig1, equalTo(datasetConfig));
    }
}
