package io.quartic.terminator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.ClientBuilder;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

import java.util.Map;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static rx.Observable.fromCallable;

@Value.Immutable
public abstract class CatalogueProxy implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueProxy.class);
    private Subscription subscription = null;

    public static ImmutableCatalogueProxy.Builder builder() {
        return ImmutableCatalogueProxy.builder();
    }

    protected static CatalogueService catalogueFromUrl(Class<?> owner, String url) {
        return ClientBuilder.build(CatalogueService.class, owner, url);
    }

    public static final int DEFAULT_POLL_PERIOD_MILLISECONDS = 2000;

    protected abstract CatalogueService catalogue();
    @Value.Default
    protected long pollPeriodMilliseconds() {
        return DEFAULT_POLL_PERIOD_MILLISECONDS;
    }

    private final Set<TerminationId> terminationIds = Sets.newHashSet();

    public void start() {
        subscription = fromCallable(() -> catalogue().getDatasets())
                .doOnError((e) -> LOG.error("Error polling catalogue: {}", e.getMessage()))
                .repeatWhen(pollDelay())
                .retryWhen(pollDelay())
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

    private Func1<Observable<?>, Observable<?>> pollDelay() {
        return o -> o.delay(pollPeriodMilliseconds(), MILLISECONDS);
    }

    public Set<TerminationId> terminationIds() {
        synchronized (terminationIds) {
            return ImmutableSet.copyOf(terminationIds);
        }
    }
}
