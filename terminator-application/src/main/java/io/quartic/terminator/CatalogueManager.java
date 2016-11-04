package io.quartic.terminator;

import io.quartic.catalogue.api.CatalogueService;
import io.quartic.common.client.ClientBuilder;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import static io.quartic.terminator.AbstractDatasetAction.ActionType.ADDED;
import static io.quartic.terminator.AbstractDatasetAction.ActionType.REMOVED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.fromCallable;

@Value.Immutable
public abstract class CatalogueManager {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueManager.class);

    public static ImmutableCatalogueManager.Builder builder() {
        return ImmutableCatalogueManager.builder();
    }

    protected static CatalogueService fromUrl(String url) {
        return ClientBuilder.build(CatalogueService.class, url);
    }

    public static final int DEFAULT_POLL_PERIOD_MILLISECONDS = 2000;

    protected abstract CatalogueService catalogue();
    @Value.Default
    protected long pollPeriodMilliseconds() {
        return DEFAULT_POLL_PERIOD_MILLISECONDS;
    }

    public Observable<DatasetAction> datasetActions() {
        return fromCallable(() -> catalogue().getDatasets())
                .doOnError((e) -> LOG.error("Error polling catalogue", e))
                .repeatWhen(o -> o.delay(pollPeriodMilliseconds(), MILLISECONDS))
                .retry()
                .flatMap(ChangeCalculator.create(
                        (id, config) -> DatasetAction.of(ADDED, id, config),
                        (id, config) -> DatasetAction.of(REMOVED, id, config)
                ));
    }
}
