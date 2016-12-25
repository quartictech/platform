package io.quartic.terminator

import io.quartic.catalogue.api.DatasetConfig
import io.quartic.catalogue.api.DatasetId
import io.quartic.catalogue.api.TerminationId
import io.quartic.catalogue.api.TerminatorDatasetLocator
import io.quartic.common.websocket.WebsocketListener
import io.quartic.common.logging.logger
import io.quartic.common.serdes.objectMapper
import rx.Subscription

class CatalogueWatcher(private val listenerFactory: WebsocketListener.Factory) : AutoCloseable {
    private val LOG by logger()

    private var subscription: Subscription? = null

    private val _terminationIds = mutableSetOf<TerminationId>()
    val terminationIds: Set<TerminationId> get() {
        synchronized(_terminationIds) {
            return _terminationIds.toSet()
        }
    }

    private val listener: WebsocketListener<Map<DatasetId, DatasetConfig>> by lazy {
        listenerFactory.create<Map<DatasetId, DatasetConfig>>(
                objectMapper().typeFactory.constructMapType(Map::class.java, DatasetId::class.java, DatasetConfig::class.java)
        )
    }

    fun start() {
        subscription = listener.observable.subscribe({ this.update(it) })
    }

    override fun close() {
        if (subscription != null) {
            subscription!!.unsubscribe()
            subscription = null
        }
    }

    private fun update(datasets: Map<DatasetId, DatasetConfig>) {
        LOG.info("Received catalogue update")
        synchronized(_terminationIds) {
            _terminationIds.clear()
            _terminationIds.addAll(
                    datasets.values
                            .filter({ config -> config.locator() is TerminatorDatasetLocator })
                            .map({ config -> (config.locator() as TerminatorDatasetLocator).id() })
            )
        }
    }
}
