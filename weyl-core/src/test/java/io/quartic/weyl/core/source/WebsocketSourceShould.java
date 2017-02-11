package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.common.websocket.WebsocketListener;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Geometry;
import io.quartic.common.geojson.Point;
import io.quartic.weyl.api.LayerUpdateType;
import io.quartic.weyl.api.LiveEvent;
import io.quartic.weyl.api.LiveEventImpl;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class WebsocketSourceShould {
    private static final FeatureCollection FEATURE_COLLECTION = featureCollection(geojsonFeature("a", point()));
    private final static LiveEvent LIVE_EVENT = LiveEventImpl.of(LayerUpdateType.APPEND, Instant.now(), FEATURE_COLLECTION);

    @Test
    public void import_things() throws Exception {
        @SuppressWarnings("unchecked")
        final WebsocketListener<LiveEvent> listener = mock(WebsocketListener.class);
        final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
        final FeatureConverter converter = mock(FeatureConverter.class);
        @SuppressWarnings("unchecked")
        final Collection<NakedFeature> modelFeatures = mock(Collection.class);

        when(listenerFactory.create(LiveEvent.class)).thenReturn(listener);
        when(listener.getObservable()).thenReturn(just(LIVE_EVENT));
        when(converter.toModel(any())).thenReturn(modelFeatures);

        final WebsocketSource source = ImmutableWebsocketSource.builder()
                .name("test")
                .converter(converter)
                .listenerFactory(listenerFactory)
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();

        TestSubscriber<LayerUpdate> subscriber = TestSubscriber.create();
        source.observable().subscribe(subscriber);
        subscriber.awaitValueCount(1, 1, TimeUnit.SECONDS);

        verify(converter).toModel(FEATURE_COLLECTION);
        assertThat(subscriber.getOnNextEvents().get(0), equalTo(LayerUpdateImpl.of(LayerUpdateType.APPEND, modelFeatures)));
    }

    // TODO: there's a lot of duplication of helper methods here (with e.g. LiveEventConverterShould)

    private static FeatureCollection featureCollection(Feature... features) {
        return new FeatureCollection(newArrayList(features));
    }

    private static Feature geojsonFeature(String id, Geometry geometry) {
        return new Feature(id, geometry, ImmutableMap.of("timestamp", 1234));
    }

    private static Point point() {
        return point(51.0, 0.1);
    }

    private static Point point(double x, double y) {
        return new Point(ImmutableList.of(x, y));
    }

}
