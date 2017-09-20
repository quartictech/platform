package io.quartic.catalogue.postgres

import com.codahale.metrics.health.HealthCheck.Result
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import java.time.Instant

class PostgresBackend(private val database: Database) : StorageBackend {
    override fun get(coords: DatasetCoordinates) = database.getDataset(coords.namespace.namespace, coords.id.uid)

    override fun set(coords: DatasetCoordinates, config: DatasetConfig) = insert(coords, config)

    override fun contains(coords: DatasetCoordinates) = (get(coords) != null)

    override fun remove(coords: DatasetCoordinates) = insert(coords, null)     // Tombstone

    override fun getAll() = database.getDatasets()
        .filter { it.config != null }
        .associateBy(
            { DatasetCoordinates(it.namespace, it.id) },
            { it.config!! }
        )

    override fun healthCheck(): Result = Result.healthy() // TODO

    private fun insert(coords: DatasetCoordinates, config: DatasetConfig?) {
        database.insertDataset(
            namespace = coords.namespace.namespace,
            id = coords.id.uid,
            config = config,
            time = Instant.now()
        )
    }
}
