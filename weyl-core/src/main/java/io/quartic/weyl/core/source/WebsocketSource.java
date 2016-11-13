package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.quartic.common.client.WebsocketListener;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.live.LiveEventConverter;
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

    protected abstract LiveEventConverter converter();
    protected abstract MetricRegistry metrics();
    protected abstract WebsocketListener.Factory listenerFactory();

    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(WebsocketSource.class, "messages", "rate"));
    }

    @Value.Lazy
    @Override
    public Observable<SourceUpdate> observable() {
        return listenerFactory().create(LiveEvent.class)
                .observable()
                .doOnNext(s -> messageRateMeter().mark())
                .map(fc -> converter().updateFrom(fc));
    }

    @Override
    public boolean indexable() {
        return false;
    }

    @Override
    public LayerViewType viewType() {
        return LayerViewType.MOST_RECENT;
    }
}
