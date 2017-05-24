package io.quartic.catalogue.datastore;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetCoordinates;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.catalogue.api.model.DatasetMetadata;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class EntitySerDeShould {
    final EntitySerDe entitySerDe = new EntitySerDe(coords -> mock(Key.class));

    @Test
    public void serialize_deserialize_correctly() throws IOException {
        DatasetConfig datasetConfig = new DatasetConfig(
                new DatasetMetadata(
                        "name",
                        "description",
                        "attribution",
                        Instant.now()
                ),
                new DatasetLocator.GeoJsonDatasetLocator("wat"),
                ImmutableMap.of("foo", "bar", "wat", ImmutableMap.of("ladispute", 1337))
        );

        Entity entity = entitySerDe.datasetToEntity(mock(DatasetCoordinates.class), datasetConfig);

        assertThat(entitySerDe.entityToDataset(entity), equalTo(datasetConfig));
    }
}
