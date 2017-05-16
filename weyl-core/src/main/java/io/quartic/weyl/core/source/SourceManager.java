package io.quartic.weyl.core.source;

import io.quartic.catalogue.CatalogueEvent;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.MapDatasetExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.observables.GroupedObservable;

import java.util.Optional;
import java.util.function.Function;

import static io.quartic.catalogue.CatalogueEvent.Type.CREATE;
import static io.quartic.catalogue.CatalogueEvent.Type.DELETE;
import static java.lang.String.format;
import static rx.Observable.empty;
import static rx.Observable.just;

public class SourceManager {
    private static final Logger LOG = LoggerFactory.getLogger(SourceManager.class);
    private final Observable<CatalogueEvent> catalogueEvents;
    private final Function<DatasetConfig, Optional<Source>> sourceFactory;
    private final Scheduler scheduler;
    private final ExtensionCodec extensionCodec;
    private Observable<LayerPopulator> layerPopulators = null;  // TODO - eliminate grossness to emulate lazy evaluation

    public SourceManager(
            Observable<CatalogueEvent> catalogueEvents,
            Function<DatasetConfig, Optional<Source>> sourceFactory,
            Scheduler scheduler
    ) {
        this(catalogueEvents, sourceFactory, scheduler, new ExtensionCodec());
    }

    public SourceManager(
            Observable<CatalogueEvent> catalogueEvents,
            Function<DatasetConfig, Optional<Source>> sourceFactory,
            Scheduler scheduler,
            ExtensionCodec extensionCodec
    ) {
        this.catalogueEvents = catalogueEvents;
        this.sourceFactory = sourceFactory;
        this.scheduler = scheduler;
        this.extensionCodec = extensionCodec;
    }

    public Observable<LayerPopulator> layerPopulators() {
        if (layerPopulators == null) {
            layerPopulators = catalogueEvents
                    .groupBy(CatalogueEvent::getId)
                    .flatMap(this::processEventsForId)
                    .share();
        }
        return layerPopulators;
    }

    private Observable<LayerPopulator> processEventsForId(GroupedObservable<DatasetId, CatalogueEvent> group) {
        final Observable<CatalogueEvent> events = group.cache();    // Because groupBy can only have one subscriber per group
        final Observable<LayerPopulator> populator = events
                .filter(e -> e.getType() == CREATE)
                .flatMap(event -> createSource(event.getId(), event.getConfig())
                        .map(source -> createPopulator(
                                event.getId(),
                                event.getConfig(),
                                sourceUntil(source, deletionEventFrom(events))))
                );

        LOG.info("[{}] Finished constructing populator", group.getKey());
        return populator;
    }

    private LayerPopulator createPopulator(DatasetId id, DatasetConfig config, Source source) {
        final String name = config.getMetadata().getName();
        final MapDatasetExtension extension = extensionCodec.decode(name, config.getExtensions());

        LOG.info(format("[%s] Created layer", name));

        return LayerPopulator.withoutDependencies(
                new LayerSpec(
                        new LayerId(id.getUid()),
                        datasetMetadataFrom(config.getMetadata()),
                        extension.getViewType().getLayerView(),
                        extension.getStaticSchema(),
                        source.indexable()
                ),
                source.observable().subscribeOn(scheduler)     // TODO: the scheduler should be chosen by the specific source;
        );
    }

    private Observable<Source> createSource(DatasetId id, DatasetConfig config) {
        try {
            Optional<Source> source = sourceFactory.apply(config);
            if (!source.isPresent()) {
                LOG.error(format("[%s] Unhandled config : %s", id, config.getLocator()));
                return empty();
            }

            return just(source.get());
        } catch (Exception e) {
            LOG.error(format("[%s] Error creating layer for dataset", id), e);
            return empty();
        }
    }

    // This mechanism causes the observable to complete, as well as unsubscription from the upstream
    private Source sourceUntil(Source source, Observable<?> until) {
        return new Source() {
            @Override
            public Observable<LayerUpdate> observable() {
                return source.observable().takeUntil(until);
            }

            @Override
            public boolean indexable() {
                return source.indexable();
            }
        };
    }

    private Observable<CatalogueEvent> deletionEventFrom(Observable<CatalogueEvent> events) {
        return events
                .filter(e -> e.getType() == DELETE)
                .doOnNext(e -> LOG.info(format("[%s] Deleted layer", e.getConfig().getMetadata().getName())));
    }

    // TODO: do we really need LayerMetadata to be distinct from DatasetMetadata?
    private LayerMetadata datasetMetadataFrom(DatasetMetadata metadata) {
        return new LayerMetadata(
                metadata.getName(),
                metadata.getDescription(),
                metadata.getAttribution(),
                metadata.getRegistered()    // Should always be non-null
        );
    }
}
