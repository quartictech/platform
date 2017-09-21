package io.quartic.catalogue

import com.fasterxml.jackson.core.JsonProcessingException
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.catalogue.database.Database
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import java.time.Clock
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Session
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotFoundException

// TODO - all this synchronisation is really lame
class CatalogueResource(
    private val database: Database,
    private val didGenerator: UidGenerator<DatasetId> = randomGenerator(::DatasetId),
    private val clock: Clock = Clock.systemUTC()
) : Endpoint(), CatalogueService {
    private val LOG by logger()
    private val sessions = mutableSetOf<Session>()    // TODO: extend ResourceManagingEndpoint instead

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
        updateClients()
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

        updateClients()
    }

    @Synchronized
    override fun onOpen(session: Session, config: EndpointConfig) {
        LOG.info("[{}] Open", session.id)
        updateClient(session, getDatasets())
        sessions.add(session)
    }

    @Synchronized
    override fun onClose(session: Session?, closeReason: CloseReason?) {
        LOG.info("[{}] Close", session!!.id)
        sessions.remove(session)
    }

    private fun updateClients() {
        val datasets = getDatasets()
        sessions.forEach { session -> updateClient(session, datasets) }
    }

    private fun updateClient(session: Session, datasets: Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>) {
        try {
            session.asyncRemote.sendText(OBJECT_MAPPER.writeValueAsString(datasets))
        } catch (e: JsonProcessingException) {
            LOG.error("Error producing JSON", e)
        }
    }
}
