package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.catalogue.api.WebsocketDatasetLocator;
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

    @Value.Default
    protected WebsocketListener listener() {
        return WebsocketListener.builder()
                .name(name())
                .url(locator().url())
                .metrics(metrics())
                .build();
    }

    @Value.Lazy
    @Override
    public Observable<SourceUpdate> observable() {
        return listener()
                .observable()
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
