package io.quartic.weyl.core.source;

import io.quartic.catalogue.CatalogueEvent;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.MapDatasetExtension;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.observables.GroupedObservable;

import java.util.Map;
import java.util.function.Function;

import static io.quartic.catalogue.CatalogueEvent.Type.CREATE;
import static io.quartic.catalogue.CatalogueEvent.Type.DELETE;
import static java.lang.String.format;
import static rx.Observable.empty;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class SourceManager {
    private static final Logger LOG = LoggerFactory.getLogger(SourceManager.class);
    protected abstract Observable<CatalogueEvent> catalogueEvents();
    protected abstract Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories();
    protected abstract Scheduler scheduler();
    @Value.Default
    protected ExtensionCodec extensionCodec() {
        return new ExtensionCodec();
    }

    @Value.Lazy
    public Observable<LayerPopulator> layerPopulators() {
        return catalogueEvents()
                .groupBy(CatalogueEvent::getId)
                .flatMap(this::processEventsForId)
                .share();
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
        final MapDatasetExtension extension = extensionCodec().decode(name, config.getExtensions());

        LOG.info(format("[%s] Created layer", name));

        return LayerPopulator.withoutDependencies(
                LayerSpecImpl.of(
                        new LayerId(id.getUid()),
                        datasetMetadataFrom(config.getMetadata()),
                        extension.viewType().getLayerView(),
                        extension.staticSchema(),
                        source.indexable()
                ),
                source.observable().subscribeOn(scheduler())     // TODO: the scheduler should be chosen by the specific source;
        );
    }

    private Observable<Source> createSource(DatasetId id, DatasetConfig config) {
        final Function<DatasetConfig, Source> func = sourceFactories().get(config.getLocator().getClass());
        if (func == null) {
            LOG.error(format("[%s] Unrecognised config type: %s", id, config.getLocator().getClass()));
            return empty();
        }

        try {
            return just(func.apply(config));
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
        return LayerMetadataImpl.builder()
                .name(metadata.getName())
                .description(metadata.getDescription())
                .attribution(metadata.getAttribution())
                .registered(metadata.getRegistered())    // Should always be non-null
                .build();
    }
}
