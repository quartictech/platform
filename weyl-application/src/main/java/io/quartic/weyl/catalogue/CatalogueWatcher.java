package io.quartic.weyl.catalogue;

import com.google.common.collect.Maps;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.client.WebsocketListener;
import io.quartic.weyl.core.SourceDescriptor;
import io.quartic.weyl.core.SourceDescriptorImpl;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.Source;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static rx.Observable.from;

@Value.Immutable
public abstract class CatalogueWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueWatcher.class);

    public static ImmutableCatalogueWatcher.Builder builder() {
        return ImmutableCatalogueWatcher.builder();
    }

    private final Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();

    protected abstract Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories();
    protected abstract Scheduler scheduler();
    protected abstract WebsocketListener.Factory listenerFactory();

    @Value.Default
    protected ExtensionParser extensionParser() {
        return new ExtensionParser();
    }

    @Value.Lazy
    protected WebsocketListener<Map<DatasetId, DatasetConfig>> listener() {
        return listenerFactory().create(OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, DatasetId.class, DatasetConfig.class));
    }

    @Value.Lazy
    public Observable<SourceDescriptor> sources() {
        return listener().observable()
                .flatMap(this::update)
                .share();
    }

    private Observable<SourceDescriptor> update(Map<DatasetId, DatasetConfig> datasets) {
        LOG.info("Received catalogue update");
        final List<SourceDescriptor> sources = datasets.entrySet().stream()
                .filter(e -> !this.datasets.containsKey(e.getKey()))
                .flatMap(e -> createSource(e.getKey(), e.getValue()))
                .collect(toList());
        // TODO: explicitly remove old layers
        this.datasets.clear();
        this.datasets.putAll(datasets);
        return from(sources);
    }

    private Stream<SourceDescriptor> createSource(DatasetId id, DatasetConfig config) {
        try {
            final Function<DatasetConfig, Source> func = sourceFactories().get(config.locator().getClass());
            if (func == null) {
                throw new IllegalArgumentException("Unrecognised config type " + config.locator().getClass());
            }

            final String name = config.metadata().name();
            final MapDatasetExtension extension = extensionParser().parse(name, config.extensions());
            final Source source = func.apply(config);

            LOG.info(format("[%s] Created layer", name));
            return Stream.of(SourceDescriptorImpl.of(
                    LayerIdImpl.of(id.uid()),
                    datasetMetadataFrom(config.metadata()),
                    extension.viewType().getLayerView(),
                    schemaFrom(extension),
                    source.indexable(),
                    source.observable().subscribeOn(scheduler())    // TODO: the scheduler should be chosen by the specific source
            ));
        } catch (Exception e) {
            LOG.error(format("[%s] Error creating layer for dataset", id), e);
            return Stream.empty();
        }
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
