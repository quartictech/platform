package io.quartic.terminator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static rx.Observable.empty;
import static rx.Observable.just;

@Value.Immutable
public abstract class CatalogueWatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueWatcher.class);
    private Subscription subscription = null;

    public static ImmutableCatalogueWatcher.Builder builder() {
        return ImmutableCatalogueWatcher.builder();
    }

    protected abstract String catalogueWatchUrl();
    protected abstract ObjectMapper objectMapper();
    protected abstract WebsocketClientSessionFactory websocketFactory();

    @Value.Default
    protected WebsocketListener listener() {
        return WebsocketListener.builder()
                .websocketFactory(websocketFactory())
                .name(this.getClass().getSimpleName())
                .url(catalogueWatchUrl())
                .build();
    }

    private final Set<TerminationId> terminationIds = Sets.newHashSet();

    public void start() {
        subscription = listener()
                .observable()
                .flatMap(this::convert)
                .subscribe(new Subscriber<Map<DatasetId, DatasetConfig>>() {
                    @Override
                    public void onCompleted() {
                        LOG.error("Unexpected call to onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error("Unexpected call to onError", e);
                    }

                    @Override
                    public void onNext(Map<DatasetId, DatasetConfig> datasets) {
                        synchronized (terminationIds) {
                            terminationIds.clear();
                            terminationIds.addAll(
                                    datasets.values().stream()
                                            .filter(config -> config.locator() instanceof TerminatorDatasetLocator)
                                            .map(config -> ((TerminatorDatasetLocator) config.locator()).id())
                                            .collect(toList())
                            );
                        }
                    }
                });
    }

    @Override
    public void close() throws Exception {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private Observable<Map<DatasetId, DatasetConfig>> convert(String message) {
        try {
            final MapType mapType = objectMapper().getTypeFactory().constructMapType(Map.class, DatasetId.class, DatasetConfig.class);
            return just(objectMapper().readValue(message, mapType));
        } catch (IOException e) {
            LOG.error("Error converting message", e);
            return empty();
        }
    }

    public Set<TerminationId> terminationIds() {
        synchronized (terminationIds) {
            return ImmutableSet.copyOf(terminationIds);
        }
    }
}
