package io.quartic.terminator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.common.client.WebsocketListener;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;

import java.util.Map;
import java.util.Set;

import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.util.stream.Collectors.toCollection;

@Value.Immutable
public abstract class CatalogueWatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueWatcher.class);

    public static CatalogueWatcher of(WebsocketListener.Factory listenerFactory) {
        return ImmutableCatalogueWatcher.of(listenerFactory);
    }

    private Subscription subscription = null;
    private final Set<TerminationId> terminationIds = Sets.newHashSet();

    @Value.Parameter
    protected abstract WebsocketListener.Factory listenerFactory();

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
        synchronized (terminationIds) {
            terminationIds.clear();
            datasets.values().stream()
                    .filter(config -> config.locator() instanceof TerminatorDatasetLocator)
                    .map(config -> ((TerminatorDatasetLocator) config.locator()).id())
                    .collect(toCollection(() -> terminationIds));
        }
    }

    public Set<TerminationId> terminationIds() {
        synchronized (terminationIds) {
            return ImmutableSet.copyOf(terminationIds);
        }
    }
}
