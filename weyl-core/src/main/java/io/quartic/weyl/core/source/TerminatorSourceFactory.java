package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.common.client.WebsocketListener;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.weyl.core.LayerUpdate;
import io.quartic.weyl.core.LayerUpdateImpl;
import io.quartic.weyl.core.feature.FeatureConverter;
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
    protected abstract WebsocketListener.Factory listenerFactory();

    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(TerminatorSourceFactory.class, "messages", "rate"));
    }

    @Value.Derived
    protected Observable<FeatureCollectionWithTerminationId> collections() {
        return listenerFactory().create(FeatureCollectionWithTerminationId.class)
                .observable()
                .doOnNext(s -> messageRateMeter().mark());
    }

    public Source sourceFor(TerminatorDatasetLocator locator, FeatureConverter converter) {
        return new Source() {
            @Override
            public Observable<LayerUpdate> observable() {
                return collections()
                        .filter(fcwi -> fcwi.terminationId().equals(locator.id()))  // TODO: this scales linearly with the number of datasets, which isn't great
                        .map(fcwdi -> LayerUpdateImpl.of(converter.toModel(fcwdi.featureCollection())));
            }

            @Override
            public boolean indexable() {
                return false;
            }
        };
    }
}
