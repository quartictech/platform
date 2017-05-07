package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.quartic.common.websocket.WebsocketListener;
import io.quartic.weyl.api.LiveEvent;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

public class WebsocketSource implements Source {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketSource.class);

    private final String name;
    private final FeatureConverter converter;
    private final WebsocketListener.Factory listenerFactory;
    private final Meter messageRateMeter;
    private final boolean indexable;

    public WebsocketSource(
            String name,
            FeatureConverter converter,
            MetricRegistry metrics,
            WebsocketListener.Factory listenerFactory,
            boolean indexable
    ) {
        this.name = name;
        this.converter = converter;
        this.listenerFactory = listenerFactory;
        this.messageRateMeter = metrics.meter(MetricRegistry.name(WebsocketSource.class, "messages", "rate", name));
        this.indexable = indexable;
    }

    @Override
    public boolean indexable() {
        return indexable;
    }

    // TODO - lazy
    @Override
    public Observable<LayerUpdate> observable() {
        return listenerFactory.create(LiveEvent.class)
                .getObservable()
                .doOnNext(s -> messageRateMeter.mark())
                .doOnNext(s -> LOG.info("[{}] received {}", name, s.getUpdateType()))
                .map(event -> new LayerUpdate(event.getUpdateType(), converter.toModel(event.getFeatureCollection())));
    }

}
