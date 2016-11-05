package io.quartic.terminator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.client.ClientBuilder;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.fromCallable;

@Value.Immutable
public abstract class CatalogueProxy {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueProxy.class);

    public static ImmutableCatalogueProxy.Builder builder() {
        return ImmutableCatalogueProxy.builder();
    }

    protected static CatalogueService catalogueFromUrl(String url) {
        return ClientBuilder.build(CatalogueService.class, url);
    }

    public static final int DEFAULT_POLL_PERIOD_MILLISECONDS = 2000;

    protected abstract CatalogueService catalogue();
    @Value.Default
    protected long pollPeriodMilliseconds() {
        return DEFAULT_POLL_PERIOD_MILLISECONDS;
    }

    private final Map<DatasetId, DatasetConfig> datasets = Maps.newHashMap();

    public void start() {
        fromCallable(() -> catalogue().getDatasets())
                .doOnError((e) -> LOG.error("Error polling catalogue", e))
                .repeatWhen(o -> o.delay(pollPeriodMilliseconds(), MILLISECONDS))
                .retry()
                .subscribe(new Subscriber<Map<DatasetId, DatasetConfig>>() {
                    @Override
                    public void onCompleted() {
                        LOG.error("Unexpectedly call to onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error("Unexpectedly call to onError", e);
                    }

                    @Override
                    public void onNext(Map<DatasetId, DatasetConfig> datasets) {
                        synchronized(CatalogueProxy.this.datasets) {
                            CatalogueProxy.this.datasets.clear();
                            CatalogueProxy.this.datasets.putAll(datasets);
                        }
                    }
                });

    }

    public Map<DatasetId, DatasetConfig> datasets() {
        synchronized (datasets) {
            return ImmutableMap.copyOf(datasets);
        }
    }
}
