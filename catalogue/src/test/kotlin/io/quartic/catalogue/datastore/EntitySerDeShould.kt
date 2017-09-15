package io.quartic.catalogue.datastore

import com.nhaarman.mockito_kotlin.mock
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import java.time.Instant

class EntitySerDeShould {
    private val entitySerDe = EntitySerDe { mock() }

    @Test
    fun serialize_deserialize_correctly() {
        val datasetConfig = DatasetConfig(
                DatasetMetadata(
                        "name",
                        "description",
                        "attribution",
                        Instant.now()
                ),
                DatasetLocator.GeoJsonDatasetLocator("wat"),
                mapOf(
                    "foo" to "bar",
                    "wat" to mapOf("ladispute" to 1337)
                )
        )

        val entity = entitySerDe.datasetToEntity(mock(), datasetConfig)

        assertThat(entitySerDe.entityToDataset(entity), equalTo(datasetConfig))
    }
}
