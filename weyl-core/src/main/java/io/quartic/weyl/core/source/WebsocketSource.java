package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.catalogue.api.WebsocketDatasetLocator;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;

import static rx.Observable.empty;
import static rx.Observable.just;

@Value.Immutable
public abstract class WebsocketSource implements Source {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketSource.class);

    public static ImmutableWebsocketSource.Builder builder() {
        return ImmutableWebsocketSource.builder();
    }

    protected abstract String name();
    protected abstract WebsocketDatasetLocator locator();
    protected abstract LiveEventConverter converter();
    protected abstract ObjectMapper objectMapper();
    protected abstract MetricRegistry metrics();
    protected abstract WebsocketClientSessionFactory websocketFactory();

    @Value.Default
    protected WebsocketListener listener() {
        return WebsocketListener.builder()
                .websocketFactory(websocketFactory())
                .name(name())
                .url(locator().url())
                .build();
    }

    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(WebsocketSource.class, "messages", "rate"));
    }

    @Value.Lazy
    @Override
    public Observable<SourceUpdate> observable() {
        return listener()
                .observable()
                .doOnNext(s -> messageRateMeter().mark())
                .flatMap(this::convert);
    }

    private Observable<SourceUpdate> convert(String message) {
        try {
            return just(converter().updateFrom(objectMapper().readValue(message, FeatureCollection.class)));
        } catch (IOException e) {
            LOG.error("Error converting message", e);
            return empty();
        }
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
