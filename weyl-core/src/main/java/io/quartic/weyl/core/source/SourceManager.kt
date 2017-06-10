package io.quartic.weyl.core.source

import io.quartic.catalogue.CatalogueEvent
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.common.logging.logger
import io.quartic.weyl.core.model.LayerId
import io.quartic.weyl.core.model.LayerMetadata
import io.quartic.weyl.core.model.LayerPopulator
import io.quartic.weyl.core.model.LayerSpec
import rx.Observable
import rx.Observable.empty
import rx.Observable.just
import rx.Scheduler
import rx.observables.GroupedObservable
import java.lang.String.format

class SourceManager @JvmOverloads constructor(
        private val catalogueEvents: Observable<CatalogueEvent>,
        private val sourceFactory: (DatasetConfig) -> Source?,
        private val scheduler: Scheduler,
        private val extensionCodec: ExtensionCodec = ExtensionCodec()
) {
    private val LOG by logger()

    val layerPopulators by lazy {
        catalogueEvents
                .groupBy { it.coords }
                .flatMap { this.processEventsForCoords(it) }
                .share()
    }

    private fun processEventsForCoords(group: GroupedObservable<DatasetCoordinates, CatalogueEvent>): Observable<LayerPopulator> {
        val events = group.cache()    // Because groupBy can only have one subscriber per group
        return events
                .filter { it.type === CREATE }
                .flatMap {
                    createSource(it.coords, it.config)
                            .map { source -> createPopulator(
                                    it.coords,
                                    it.config,
                                    sourceUntil(source, deletionEventFrom(events))
                            )}
                            .flatMap { s -> s }
                }
    }

    private fun createPopulator(coords: DatasetCoordinates, config: DatasetConfig, source: Source): Observable<LayerPopulator> {
        val name = config.metadata.name
        val extension = extensionCodec.decode(name, config.extensions)

        return if (extension == null) {
            empty()
        } else {
            LOG.info("[$name] Created layer")
            just(LayerPopulator.withoutDependencies(
                    LayerSpec(
                            LayerId(coords.toString()), // // TODO - deal with DatasetCoords properly
                            datasetMetadataFrom(config.metadata),
                            extension.viewType.layerView,
                            extension.staticSchema,
                            source.indexable()
                    ),
                    source.observable().subscribeOn(scheduler)     // TODO: the scheduler should be chosen by the specific source;
            ))
        }
    }

    private fun createSource(coords: DatasetCoordinates, config: DatasetConfig): Observable<Source> {
        try {
            val source = sourceFactory(config)
            if (source != null) {
                return just(source)
            }
            LOG.error("[$coords] Unhandled config : ${config.locator}")
        } catch (e: Exception) {
            LOG.error("[$coords] Error creating layer for dataset", e)
        }
        return empty()
    }

    // This mechanism causes the observable to complete, as well as unsubscription from the upstream
    private fun sourceUntil(source: Source, until: Observable<*>) = object : Source {
        override fun observable() = source.observable().takeUntil(until)
        override fun indexable() = source.indexable()
    }

    private fun deletionEventFrom(events: Observable<CatalogueEvent>) = events
            .filter { it.type === DELETE }
            .doOnNext { LOG.info("[${it.config.metadata.name}] Deleted layer") }

    // TODO: do we really need LayerMetadata to be distinct from DatasetMetadata?
    private fun datasetMetadataFrom(metadata: DatasetMetadata) = LayerMetadata(
            metadata.name,
            metadata.description,
            metadata.attribution,
            metadata.registered!!    // Should always be non-null
    )
}
