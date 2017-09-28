package io.quartic.catalogue

import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.catalogue.database.Database
import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import java.time.Clock
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException

// TODO - all this synchronisation is really lame
class CatalogueResource(
    private val database: Database,
    private val didGenerator: UidGenerator<DatasetId> = randomGenerator(::DatasetId),
    private val clock: Clock = Clock.systemUTC()
) : CatalogueService {
    override fun registerDataset(namespace: DatasetNamespace, config: DatasetConfig): DatasetCoordinates {
        return registerOrUpdateDataset(namespace, didGenerator.get(), config)
    }

    @Synchronized
    override fun registerOrUpdateDataset(namespace: DatasetNamespace, id: DatasetId, config: DatasetConfig): DatasetCoordinates {
        val coords = DatasetCoordinates(namespace, id)
        if (config.metadata.registered != null) {
            throw BadRequestException("'registered' field should not be present")
        }

        // TODO: basic validation
        database.insertDataset(
            namespace = namespace.namespace,
            id = id.uid,
            config = withRegisteredTimestamp(config)
        )
        return coords
    }

    private fun withRegisteredTimestamp(config: DatasetConfig) =
        config.copy(metadata = config.metadata.copy(registered = clock.instant()))

    @Synchronized
    override fun getDatasets(): Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> =
        database.getDatasets()
            .groupBy { DatasetNamespace(it.namespace) }
            .mapValues {
                it.value.associateBy(
                    { DatasetId(it.id) },
                    { it.config }
                )
            }

    @Synchronized
    override fun getDataset(namespace: DatasetNamespace, id: DatasetId) =
        database.getDataset(namespace.namespace, id.uid)
            ?: throw NotFoundException("No dataset: ${DatasetCoordinates(namespace, id)}")

    @Synchronized
    override fun deleteDataset(namespace: DatasetNamespace, id: DatasetId) {
        getDataset(namespace, id)   // In order to cause exception if dataset doesn't exist

        database.deleteDataset(
            namespace = namespace.namespace,
            id = id.uid
        )
    }
}
