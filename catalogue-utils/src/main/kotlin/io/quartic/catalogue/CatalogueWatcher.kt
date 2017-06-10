package io.quartic.catalogue

import com.google.common.collect.Maps.difference
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.logging.logger
import io.quartic.common.rx.WithPrevious
import io.quartic.common.rx.pairWithPrevious
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.websocket.WebsocketListener
import rx.Observable
import rx.Observable.from
import rx.Observable.merge

class CatalogueWatcher(
        listenerFactory: WebsocketListener.Factory,
        private val namespaces: Set<DatasetNamespace>
) {
    private val listener by lazy {
        val tf = OBJECT_MAPPER.typeFactory
        listenerFactory.create<Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>>(
                tf.constructMapType(Map::class.java,
                        tf.uncheckedSimpleType(DatasetNamespace::class.java),
                        tf.constructMapType(Map::class.java, DatasetId::class.java, DatasetConfig::class.java)
                )
        )
    }

    // TODO - what about colliding IDs?
    val events: Observable<CatalogueEvent>
        get() = listener.observable
                .doOnNext { _ -> LOG.info("Received catalogue update") }
                .map { update -> update
                        .filterKeys { namespaces.contains(it) }
                        .values
                        .map { it.entries }
                        .flatten()
                        .associateBy({ it.key }, { it.value })
                }
                .compose(pairWithPrevious(emptyMap<DatasetId, DatasetConfig>()))
                .concatMap { extractEvents(it) }

    private fun extractEvents(pair: WithPrevious<Map<DatasetId, DatasetConfig>>): Observable<CatalogueEvent> {
        val diff = difference(pair.prev!!, pair.current!!)
        return merge(
                from(diff.entriesOnlyOnLeft().entries).map({ e -> CatalogueEvent(DELETE, e.key, e.value) }),
                from(diff.entriesOnlyOnRight().entries).map({ e -> CatalogueEvent(CREATE, e.key, e.value) })
        )
    }

    companion object {
        private val LOG by logger()
    }
}
