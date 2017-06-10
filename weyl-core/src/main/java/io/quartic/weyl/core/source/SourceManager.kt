package io.quartic.weyl.core.source

import io.quartic.catalogue.CatalogueEvent
import io.quartic.catalogue.CatalogueEvent.Type.CREATE
import io.quartic.catalogue.CatalogueEvent.Type.DELETE
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
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
import java.util.*
import java.util.function.Function

class SourceManager @JvmOverloads constructor(
        private val catalogueEvents: Observable<CatalogueEvent>,
        private val sourceFactory: Function<DatasetConfig, Optional<Source>>,
        private val scheduler: Scheduler,
        private val extensionCodec: ExtensionCodec = ExtensionCodec()
) {
    private val LOG by logger()

    val layerPopulators by lazy {
        catalogueEvents
                .groupBy { it.id }
                .flatMap { this.processEventsForId(it) }
                .share()
    }

    private fun processEventsForId(group: GroupedObservable<DatasetId, CatalogueEvent>): Observable<LayerPopulator> {
        val events = group.cache()    // Because groupBy can only have one subscriber per group
        return events
                .filter { it.type === CREATE }
                .flatMap {
                    createSource(it.id, it.config)
                            .map { source -> createPopulator(
                                    it.id,
                                    it.config,
                                    sourceUntil(source, deletionEventFrom(events))
                            )}
                }
    }

    private fun createPopulator(id: DatasetId, config: DatasetConfig, source: Source): LayerPopulator {
        val name = config.metadata.name
        val extension = extensionCodec.decode(name, config.extensions)

        LOG.info("[$name] Created layer")
        return LayerPopulator.withoutDependencies(
                LayerSpec(
                        LayerId(id.uid),
                        datasetMetadataFrom(config.metadata),
                        extension.viewType.layerView,
                        extension.staticSchema,
                        source.indexable()
                ),
                source.observable().subscribeOn(scheduler)     // TODO: the scheduler should be chosen by the specific source;
        );
    }

    private fun createSource(id: DatasetId, config: DatasetConfig): Observable<Source> {
        try {
            val source = sourceFactory.apply(config)
            if (source.isPresent) {
                return just(source.get())
            }
            LOG.error("[$id] Unhandled config : ${config.locator}")
        } catch (e: Exception) {
            LOG.error("[$id] Error creating layer for dataset", e)
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
