package io.quartic.catalogue.inmemory

import com.codahale.metrics.health.HealthCheck.Result
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates

class InMemoryStorageBackend : StorageBackend {
    private val datasets = mutableMapOf<DatasetCoordinates, DatasetConfig>()

    @Synchronized
    override fun get(coords: DatasetCoordinates): DatasetConfig? = datasets[coords]

    @Synchronized
    override fun set(coords: DatasetCoordinates, config: DatasetConfig) {
        datasets[coords] = config
    }

    @Synchronized
    override fun contains(coords: DatasetCoordinates) = coords in datasets

    @Synchronized
    override fun remove(coords: DatasetCoordinates) {
        datasets.remove(coords)
    }

    @Synchronized
    override fun getAll(): Map<DatasetCoordinates, DatasetConfig> = HashMap(datasets)

    @Synchronized
    override fun healthCheck(): Result = Result.healthy() // can't really be any other way
}
