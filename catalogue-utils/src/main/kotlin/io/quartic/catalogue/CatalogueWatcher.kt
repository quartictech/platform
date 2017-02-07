package io.quartic.catalogue

import com.google.common.collect.Maps.difference
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.DatasetConfig
import io.quartic.catalogue.api.DatasetId
import io.quartic.common.logging.logger
import io.quartic.common.rx.WithPrevious
import io.quartic.common.rx.pairWithPrevious
import io.quartic.common.serdes.objectMapper
import io.quartic.common.websocket.WebsocketListener
import rx.Observable
import rx.Observable.from
import rx.Observable.merge

class CatalogueWatcher(listenerFactory: WebsocketListener.Factory) {
    private val listener by lazy {
        listenerFactory.create<Map<DatasetId, DatasetConfig>>(
                objectMapper().typeFactory.constructMapType(Map::class.java, DatasetId::class.java, DatasetConfig::class.java)
        )
    }

    val events: Observable<CatalogueEvent>
        get() = listener.observable
                .doOnNext { x -> LOG.info("Received catalogue update") }
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
