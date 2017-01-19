package io.quartic.weyl.core.source;

import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.catalogue.CatalogueEvent;
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

import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.CREATE;
import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.DELETE;
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
                .groupBy(CatalogueEvent::id)
                .flatMap(this::processEventsForId)
                .share();
    }

    private Observable<LayerPopulator> processEventsForId(GroupedObservable<DatasetId, CatalogueEvent> group) {
        final Observable<CatalogueEvent> events = group.cache();    // Because groupBy can only have one subscriber per group

        return events
                .filter(e -> e.type() == CREATE)
                .flatMap(event -> createSource(event.id(), event.config())
                        .map(source -> createPopulator(
                                event.id(),
                                event.config(),
                                sourceUntil(source, events.filter(e -> e.type() == DELETE))))
                );
    }

    private LayerPopulator createPopulator(DatasetId id, DatasetConfig config, Source source) {
        final String name = config.metadata().name();
        final MapDatasetExtension extension = extensionCodec().decode(name, config.extensions());

        LOG.info(format("[%s] Created layer", name));

        return LayerPopulator.withoutDependencies(
                LayerSpecImpl.of(
                        new LayerId(id.getUid()),
                        datasetMetadataFrom(config.metadata()),
                        extension.viewType().getLayerView(),
                        extension.staticSchema(),
                        source.indexable()
                ),
                source.observable().subscribeOn(scheduler())     // TODO: the scheduler should be chosen by the specific source;
        );
    }

    private Observable<Source> createSource(DatasetId id, DatasetConfig config) {
        final Function<DatasetConfig, Source> func = sourceFactories().get(config.locator().getClass());
        if (func == null) {
            LOG.error(format("[%s] Unrecognised config type: %s", id, config.locator().getClass()));
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

    // TODO: do we really need LayerMetadata to be distinct from DatasetMetadata?
    private LayerMetadata datasetMetadataFrom(DatasetMetadata metadata) {
        return LayerMetadataImpl.builder()
                .name(metadata.name())
                .description(metadata.description())
                .attribution(metadata.attribution())
                .registered(metadata.registered().get())    // Should always be set
                .icon(metadata.icon())
                .build();
    }
}
