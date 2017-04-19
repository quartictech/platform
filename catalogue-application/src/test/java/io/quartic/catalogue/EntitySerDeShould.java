package io.quartic.catalogue;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.catalogue.api.GeoJsonDatasetLocator;
import io.quartic.catalogue.datastore.EntitySerDe;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class EntitySerDeShould {
    KeyFactory keyFactory = new KeyFactory("test")
            .setKind("dataset");
    EntitySerDe entitySerDe = new EntitySerDe(keyFactory);

    @Test
    public void serialize_deserialize_correctly() throws IOException {
        DatasetConfig datasetConfig = new DatasetConfig(
                new DatasetMetadata(
                        "name",
                        "description",
                        "attribution",
                        Instant.now()
                ),
                new GeoJsonDatasetLocator("wat"),
                ImmutableMap.of("foo", "bar", "wat", ImmutableMap.of("ladispute", 1337))
        );

        Entity entity = entitySerDe.datasetToEntity(new DatasetId("sweet"), datasetConfig);
        DatasetConfig datasetConfig1 = entitySerDe.entityToDataset(entity);

        assertThat(datasetConfig1, equalTo(datasetConfig));
    }
}
