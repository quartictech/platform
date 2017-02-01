package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.quartic.common.websocket.WebsocketListener;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

@Value.Immutable
public abstract class WebsocketSource implements Source {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketSource.class);

    public static ImmutableWebsocketSource.Builder builder() {
        return ImmutableWebsocketSource.builder();
    }

    protected abstract String name();
    protected abstract FeatureConverter converter();
    protected abstract MetricRegistry metrics();
    protected abstract WebsocketListener.Factory listenerFactory();

    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(WebsocketSource.class, "messages", "rate", name()));
    }

    @Value.Lazy
    @Override
    public Observable<LayerUpdate> observable() {
        return listenerFactory().create(LiveEvent.class)
                .getObservable()
                .doOnNext(s -> messageRateMeter().mark())
                .map(event -> LayerUpdateImpl.of(LayerUpdate.Type.APPEND, converter().toModel(event.featureCollection())));
    }

    @Override
    public boolean indexable() {
        return false;
    }
}
