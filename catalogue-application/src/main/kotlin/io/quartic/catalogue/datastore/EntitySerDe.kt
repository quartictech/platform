package io.quartic.catalogue.datastore

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.datastore.Blob
import com.google.cloud.datastore.DateTime
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.serdes.objectMapper
import java.io.IOException
import java.time.Instant
import java.util.*

class EntitySerDe(private val keyFromCoords: (DatasetCoordinates) -> Key) {

    fun entityToDataset(entity: Entity): DatasetConfig {
        throwIfVersionMismatch(entity.getLong(VERSION))
        val dt = entity.getDateTime(REGISTERED)
        val metadata = DatasetMetadata(
                entity.getString(NAME),
                entity.getString(DESCRIPTION),
                entity.getString(ATTRIBUTION),
                if (dt == null) null else Instant.ofEpochMilli(dt.timestampMillis)
        )
        return DatasetConfig(
                metadata,
                OBJECT_MAPPER.readValue<DatasetLocator>(entity.getBlob(LOCATOR).asInputStream()),
                OBJECT_MAPPER.readValue<Map<String, Any>>(entity.getBlob(EXTENSIONS).asInputStream())
        )
    }

    private fun throwIfVersionMismatch(version: Long?) {
        if (version != CURRENT_VERSION) {
            throw IOException("Version mismatch: database has $version, code has $CURRENT_VERSION. Time to write some migrations!")
        }
    }

    fun datasetToEntity(coords: DatasetCoordinates, datasetConfig: DatasetConfig): Entity {
        return with (Entity.newBuilder(keyFromCoords(coords))) {
            set(VERSION, CURRENT_VERSION)
            set(NAME, datasetConfig.metadata.name)
            set(DESCRIPTION, datasetConfig.metadata.description)
            set(ATTRIBUTION, datasetConfig.metadata.attribution)
            if (datasetConfig.metadata.registered != null) {
                set(REGISTERED, DateTime.copyFrom(Date(datasetConfig.metadata.registered!!.toEpochMilli())))
            } else {
                setNull(REGISTERED)
            }

            set(LOCATOR, Blob.copyFrom(objectMapper().writeValueAsBytes(datasetConfig.locator)))
            set(EXTENSIONS, Blob.copyFrom(objectMapper().writeValueAsBytes(datasetConfig.extensions)))
            build()
        }
    }

    companion object {
        private val VERSION = "version"
        private val NAME = "name"
        private val DESCRIPTION = "description"
        private val ATTRIBUTION = "attribution"
        private val REGISTERED = "registered"
        private val LOCATOR = "locator"
        private val EXTENSIONS = "extensions"
        private val CURRENT_VERSION = 2L
    }
}
