package io.quartic.weyl.catalogue;

import com.google.common.collect.Maps;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.client.WebsocketListener;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.SourceUpdate;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;

import java.util.Map;
import java.util.function.Function;

import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.lang.String.format;

@Value.Immutable
public abstract class CatalogueWatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueWatcher.class);

    public static ImmutableCatalogueWatcher.Builder builder() {
        return ImmutableCatalogueWatcher.builder();
    }

    private Subscription subscription = null;
    private final Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();

    protected abstract Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> sourceFactories();
    protected abstract LayerStore layerStore();
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

    public void start() {
        subscription = listener()
                .observable()
                .subscribe(this::update);
    }

    @Override
    public void close() throws Exception {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private void update(Map<DatasetId, DatasetConfig> datasets) {
        LOG.info("Received catalogue update");
        datasets.entrySet().stream()
                .filter(e -> !this.datasets.containsKey(e.getKey()))
                .forEach(e -> createAndImportLayer(e.getKey(), e.getValue()));
        // TODO: explicitly delete old layers
        this.datasets.clear();
        this.datasets.putAll(datasets);
    }

    private void createAndImportLayer(DatasetId id, DatasetConfig config) {
        try {
            final Function<DatasetConfig, Source> func = sourceFactories().get(config.locator().getClass());
            if (func == null) {
                throw new IllegalArgumentException("Unrecognised config type " + config.locator().getClass());
            }

            final String name = config.metadata().name();
            final MapDatasetExtension extension = extensionParser().parse(name, config.extensions());
            final Source source = func.apply(config);
            final LayerId layerId = LayerId.of(id.uid());
            final Subscriber<SourceUpdate> subscriber = layerStore().createLayer(
                    layerId,
                    datasetMetadataFrom(config.metadata()),
                    extension.viewType().getLayerView(),
                    AttributeSchema.builder()
                            .imageAttribute(extension.imageAttribute())
                            .blessedAttributes(extension.blessedAttributes())
                            .build(),
                    source.indexable()
            );

            LOG.info(format("[%s] Created layer", name));

            source.observable().subscribeOn(scheduler()).subscribe(subscriber);   // TODO: the scheduler should be chosen by the specific source
        } catch (Exception e) {
            LOG.error(format("[%s] Error creating layer for dataset", id), e);
        }
    }

    // TODO: do we really need LayerMetadata to be distinct from DatasetMetadata?
    private LayerMetadata datasetMetadataFrom(DatasetMetadata metadata) {
        return LayerMetadata.builder()
                .name(metadata.name())
                .description(metadata.description())
                .attribution(metadata.attribution())
                .icon(metadata.icon())
                .build();
    }
}
