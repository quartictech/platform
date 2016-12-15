package io.quartic.weyl.core.catalogue;

import com.google.common.collect.MapDifference;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.SweetStyle;
import io.quartic.common.client.WebsocketListener;
import io.quartic.common.rx.RxUtils.WithPrevious;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.difference;
import static io.quartic.common.rx.RxUtils.pairWithPrevious;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.CREATE;
import static io.quartic.weyl.core.catalogue.CatalogueEvent.Type.DELETE;
import static rx.Observable.from;
import static rx.Observable.merge;

@SweetStyle
@Value.Immutable
public abstract class CatalogueWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueWatcher.class);

    protected abstract WebsocketListener.Factory listenerFactory();

    @Value.Lazy
    protected WebsocketListener<Map<DatasetId, DatasetConfig>> listener() {
        return listenerFactory().create(OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, DatasetId.class, DatasetConfig.class));
    }

    @Value.Lazy
    public Observable<CatalogueEvent> events() {
        return listener().observable()
                .doOnNext((x) -> LOG.info("Received catalogue update"))
                .compose(pairWithPrevious(Collections.<DatasetId, DatasetConfig>emptyMap()))
                .concatMap(this::extractEvents);
    }

    private Observable<CatalogueEvent> extractEvents(WithPrevious<Map<DatasetId, DatasetConfig>> pair) {
        final MapDifference<DatasetId, DatasetConfig> diff = difference(pair.prev(), pair.current());
        return merge(
                from(diff.entriesOnlyOnLeft().entrySet()).map(e -> CatalogueEventImpl.of(DELETE, e.getKey(), e.getValue())),
                from(diff.entriesOnlyOnRight().entrySet()).map(e -> CatalogueEventImpl.of(CREATE, e.getKey(), e.getValue()))
        );
    }
}