package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.common.client.WebsocketListener;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Geometry;
import io.quartic.geojson.Point;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class WebsocketSourceShould {
    private final static LiveEvent LIVE_EVENT = LiveEvent.of(
            Instant.now(),
            Optional.of(featureCollection(geojsonFeature("a", Optional.of(point())))),
            Optional.empty());

    @Test
    public void import_things() throws Exception {
        final WebsocketListener<LiveEvent> listener = mock(WebsocketListener.class);
        final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
        final LiveEventConverter converter = mock(LiveEventConverter.class);
        final SourceUpdate update = SourceUpdate.of(newArrayList(), newArrayList());

        when(listenerFactory.create(LiveEvent.class)).thenReturn(listener);
        when(listener.observable()).thenReturn(just(LIVE_EVENT));
        when(converter.updateFrom(any(LiveEvent.class))).thenReturn(update);

        final WebsocketSource source = ImmutableWebsocketSource.builder()
                .converter(converter)
                .listenerFactory(listenerFactory)
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();

        TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        source.observable().subscribe(subscriber);
        subscriber.awaitValueCount(1, 1, TimeUnit.SECONDS);

        verify(converter).updateFrom(LIVE_EVENT);
        assertThat(subscriber.getOnNextEvents().get(0), is(update));
    }

    // TODO: there's a lot of duplication of helper methods here (with e.g. LiveEventConverterShould)

    private static FeatureCollection featureCollection(Feature... features) {
        return FeatureCollection.of(newArrayList(features));
    }

    private static Feature geojsonFeature(String id, Optional<Geometry> geometry) {
        return Feature.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private static Point point() {
        return point(51.0, 0.1);
    }

    private static Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }

}
