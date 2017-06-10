package io.quartic.weyl.core.source

import com.google.common.collect.ImmutableMap
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.MimeType
import io.quartic.common.serdes.objectMapper
import io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK
import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.AttributeType
import io.quartic.weyl.core.model.MapDatasetExtension
import io.quartic.weyl.core.model.StaticSchema
import io.quartic.weyl.core.source.ExtensionCodec.Companion.EXTENSION_KEY
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.IOException
import java.util.Collections.emptyMap

class ExtensionCodecShould {
    private val codec = ExtensionCodec()

    @Test
    fun return_null_when_not_present() {
        assertThat(codec.decode("foo", emptyMap()), nullValue())
    }

    @Test
    fun return_null_when_unparseable() {
        assertThat(codec.decode("foo", mapOf(EXTENSION_KEY to "stuff")), nullValue())
    }

    @Test
    fun return_extension_when_parseable() {
        val raw = ImmutableMap.of<String, Any>("viewType", "LOCATION_AND_TRACK")

        assertThat(codec.decode("foo", mapOf(EXTENSION_KEY to raw)),
                equalTo(MapDatasetExtension(StaticSchema(), LOCATION_AND_TRACK)))
    }

    @Test
    fun return_extension_when_parseable_2() {
        val raw = mapOf(
                "viewType" to "LOCATION_AND_TRACK",
                "titleAttribute" to "foo"
        )

        assertThat(codec.decode("foo", mapOf(EXTENSION_KEY to raw)),
                equalTo(MapDatasetExtension(StaticSchema(AttributeName("foo")), LOCATION_AND_TRACK)))
    }

    @Test
    fun deserialize_attribute_types() {
        val raw = mapOf(
                "viewType" to "LOCATION_AND_TRACK",
                "attributeTypes" to mapOf("foo" to "TIMESTAMP")
        )

        assertThat(codec.decode("foo", mapOf(EXTENSION_KEY to raw)),
                equalTo(MapDatasetExtension(
                        StaticSchema(attributeTypes = mapOf(AttributeName("foo") to AttributeType.TIMESTAMP)),
                        LOCATION_AND_TRACK
                )))
    }

    @Test
    @Throws(IOException::class)
    fun unparse_to_original() {
        val extension = MapDatasetExtension(
                StaticSchema(
                        titleAttribute = AttributeName("test"),
                        attributeTypes = mapOf(AttributeName("foo") to AttributeType.TIMESTAMP)
                ),
                LOCATION_AND_TRACK)
        val datasetConfig = DatasetConfig(
                DatasetMetadata("foo", "wat", "nope", null),
                DatasetLocator.CloudDatasetLocator("test", false, MimeType.RAW),
                codec.encode(extension))

        val json = objectMapper().writeValueAsString(datasetConfig)
        val datasetConfigDeserialized = objectMapper().readValue(json, DatasetConfig::class.java)
        assertThat(datasetConfigDeserialized, equalTo(datasetConfig))
    }
}
