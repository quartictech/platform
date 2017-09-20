package io.quartic.catalogue.postgres

import com.codahale.metrics.health.HealthCheck.Result
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates

class PostgresBackend(private val database: Database) : StorageBackend {
    override fun get(coords: DatasetCoordinates) = database.getDataset(coords.namespace.namespace, coords.id.uid)

    override fun set(coords: DatasetCoordinates, config: DatasetConfig) = database.insertDataset(
        namespace = coords.namespace.namespace,
        id = coords.id.uid,
        config = config
    )

    override fun contains(coords: DatasetCoordinates) = (get(coords) != null)

    override fun remove(coords: DatasetCoordinates) = database.deleteDataset(
        namespace = coords.namespace.namespace,
        id = coords.id.uid
    )

    override fun getAll() = database.getDatasets()
        .filter { it.config != null }
        .associateBy(
            { DatasetCoordinates(it.namespace, it.id) },
            { it.config!! }
        )

    override fun healthCheck(): Result = Result.healthy() // TODO
}
