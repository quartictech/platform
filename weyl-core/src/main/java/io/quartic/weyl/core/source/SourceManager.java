package io.quartic.weyl.core.source;

import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.catalogue.CatalogueEvent;
import io.quartic.weyl.core.model.*;
import org.apache.commons.lang3.tuple.Pair;
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
    protected ExtensionParser extensionParser() {
        return new ExtensionParser();
    }

    @Value.Lazy
    public Observable<SourceDescriptor> sources() {
        return catalogueEvents()
                .groupBy(CatalogueEvent::id)
                .flatMap(this::processEventsForId)
                .share();
    }

    private Observable<SourceDescriptor> processEventsForId(GroupedObservable<DatasetId, CatalogueEvent> group) {
        final Observable<CatalogueEvent> events = group.cache();    // Because groupBy can only have one subscriber per group

        return events
                .filter(e -> e.type() == CREATE)
                .flatMap(event -> createSource(event.id(), event.config())
                        .map(s -> sourceUntil(s, events.filter(e -> e.type() == DELETE)))
                        .map(s -> Pair.of(event, s))
                )
                .map(p -> createDescriptor(p.getLeft().id(), p.getLeft().config(), p.getRight()));
    }

    private SourceDescriptor createDescriptor(DatasetId id, DatasetConfig config, Source source) {
        final String name = config.metadata().name();
        final MapDatasetExtension extension = extensionParser().parse(name, config.extensions());

        LOG.info(format("[%s] Created layer", name));
        return SourceDescriptorImpl.of(
                LayerIdImpl.of(id.uid()),
                datasetMetadataFrom(config.metadata()),
                extension.viewType().getLayerView(),
                schemaFrom(extension),
                source.indexable(),
                source.observable().subscribeOn(scheduler())    // TODO: the scheduler should be chosen by the specific source
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
            public Observable<SourceUpdate> observable() {
                return source.observable().takeUntil(until);
            }

            @Override
            public boolean indexable() {
                return source.indexable();
            }
        };
    }

    private AttributeSchema schemaFrom(MapDatasetExtension extension) {
        return AttributeSchemaImpl.builder()
                .titleAttribute(extension.titleAttribute())
                .imageAttribute(extension.imageAttribute())
                .blessedAttributes(extension.blessedAttributes())
                .build();
    }

    // TODO: do we really need LayerMetadata to be distinct from DatasetMetadata?
    private LayerMetadata datasetMetadataFrom(DatasetMetadata metadata) {
        return LayerMetadataImpl.builder()
                .name(metadata.name())
                .description(metadata.description())
                .attribution(metadata.attribution())
                .icon(metadata.icon())
                .build();
    }
}
