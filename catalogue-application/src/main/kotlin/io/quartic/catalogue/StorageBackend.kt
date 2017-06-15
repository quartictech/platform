package io.quartic.catalogue

import com.codahale.metrics.health.HealthCheck
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates

interface StorageBackend {
    operator fun get(coords: DatasetCoordinates): DatasetConfig?
    operator fun set(coords: DatasetCoordinates, config: DatasetConfig)
    operator fun contains(coords: DatasetCoordinates): Boolean
    fun remove(coords: DatasetCoordinates)
    fun getAll(): Map<DatasetCoordinates, DatasetConfig>
    fun healthCheck(): HealthCheck.Result
}
