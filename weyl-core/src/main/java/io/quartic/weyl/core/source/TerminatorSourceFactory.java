package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.common.client.WebsocketListener;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

@Value.Immutable
public abstract class TerminatorSourceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TerminatorSourceFactory.class);

    public static ImmutableTerminatorSourceFactory.Builder builder() {
        return ImmutableTerminatorSourceFactory.builder();
    }

    protected abstract MetricRegistry metrics();
    protected abstract LiveEventConverter converter();
    protected abstract WebsocketListener<FeatureCollectionWithTerminationId> listener();

    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(TerminatorSourceFactory.class, "messages", "rate"));
    }

    @Value.Derived
    protected Observable<FeatureCollectionWithTerminationId> collections() {
        return listener()
                .observable()
                .doOnNext(s -> messageRateMeter().mark());
    }

    public Source sourceFor(TerminatorDatasetLocator locator) {
        return new Source() {
            @Override
            public Observable<SourceUpdate> observable() {
                return collections()
                        .filter(fcwi -> fcwi.terminationId().equals(locator.id()))  // TODO: this scales linearly with the number of datasets, which isn't great
                        .map(fcwdi -> converter().updateFrom(fcwdi.featureCollection()));
            }

            @Override
            public boolean indexable() {
                return false;
            }

            @Override
            public LayerViewType viewType() {
                return LayerViewType.MOST_RECENT;
            }
        };
    }
}
