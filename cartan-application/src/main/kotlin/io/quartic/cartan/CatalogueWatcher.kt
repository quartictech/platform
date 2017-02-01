package io.quartic.cartan

import io.quartic.catalogue.api.DatasetConfig
import io.quartic.catalogue.api.DatasetId
import io.quartic.catalogue.api.GooglePubsubDatasetLocator
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.websocket.WebsocketListener
import rx.Subscription

class CatalogueWatcher(private val listenerFactory: WebsocketListener.Factory) : AutoCloseable {
    private val LOG by logger()

    private var subscription: Subscription? = null

    private val _pubsubTopics = mutableSetOf<String>()
    val pubsubTopics: Set<String> get() {
        synchronized(_pubsubTopics) {
            return _pubsubTopics.toSet()
        }
    }

    private val listener: WebsocketListener<Map<DatasetId, DatasetConfig>> by lazy {
        listenerFactory.create<Map<DatasetId, DatasetConfig>>(
                OBJECT_MAPPER.typeFactory.constructMapType(Map::class.java, DatasetId::class.java, DatasetConfig::class.java)
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
        synchronized(_pubsubTopics) {
            _pubsubTopics.clear()
            _pubsubTopics.addAll(
                    datasets.values
                            .filter({ config -> config.locator() is GooglePubsubDatasetLocator })
                            .map({ config -> (config.locator() as GooglePubsubDatasetLocator).topic() })
            )
        }
    }
}
