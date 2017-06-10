package io.quartic.weyl.core.source

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.collect.ImmutableMap
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.weyl.core.live.LayerViewType
import io.quartic.weyl.core.model.AttributeName
import io.quartic.weyl.core.model.AttributeType
import io.quartic.weyl.core.model.MapDatasetExtension
import io.quartic.weyl.core.model.StaticSchema

class ExtensionCodec {
    private val LOG by logger()

    // TODO: super-annoying duplication because @JsonUnwrapped doesn't work properly (see https://github.com/FasterXML/jackson-module-kotlin/issues/50)
    data class RawMapDatasetExtension(
            val titleAttribute: AttributeName? = null,
            val primaryAttribute: AttributeName? = null,
            val imageAttribute: AttributeName? = null,
            val blessedAttributes: List<AttributeName> = emptyList(),       // Order is important here
            val categoricalAttributes: Set<AttributeName> = emptySet(),
            val attributeTypes: Map<AttributeName, AttributeType> = emptyMap(),
            val viewType: LayerViewType = LayerViewType.MOST_RECENT
    )

    fun decode(name: String, extensions: Map<String, Any>): MapDatasetExtension? {
        val extension = extensions[EXTENSION_KEY]
        if (extension != null) {
            try {
                val raw = OBJECT_MAPPER.convertValue<RawMapDatasetExtension>(extension)
                return MapDatasetExtension(
                        StaticSchema(
                                raw.titleAttribute,
                                raw.primaryAttribute,
                                raw.imageAttribute,
                                raw.blessedAttributes,
                                raw.categoricalAttributes,
                                raw.attributeTypes
                        ),
                        raw.viewType
                )
            } catch (e: IllegalArgumentException) {
                LOG.warn("[$name] Unable to interpret extension", e)
                return null
            }
        }
        return null
    }

    fun encode(extension: MapDatasetExtension): Map<String, Any> {
        return ImmutableMap.of(EXTENSION_KEY, OBJECT_MAPPER.convertValue(extension, object : TypeReference<Map<String, Any>>() {}))
    }

    companion object {
        val EXTENSION_KEY = "map"
    }

}
