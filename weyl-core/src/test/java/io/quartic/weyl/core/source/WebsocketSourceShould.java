package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.WebsocketDatasetLocator;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Geometry;
import io.quartic.geojson.Point;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class WebsocketSourceShould {
    final static FeatureCollection FEATURE_COLLECTION = featureCollection(geojsonFeature("a", Optional.of(point())));

    @Test
    public void import_things() throws Exception {
        final WebsocketListener listener = mock(WebsocketListener.class);
        final LiveEventConverter converter = mock(LiveEventConverter.class);
        final SourceUpdate update = SourceUpdate.of(newArrayList(), newArrayList());

        when(listener.observable()).thenReturn(just(OBJECT_MAPPER.writeValueAsString(FEATURE_COLLECTION)));
        when(converter.updateFrom(any(FeatureCollection.class))).thenReturn(update);

        final WebsocketSource source = ImmutableWebsocketSource.builder()
                .name("Budgie")
                .converter(converter)
                .objectMapper(OBJECT_MAPPER)
                .listener(listener)
                .locator(WebsocketDatasetLocator.of("whatever"))
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .websocketFactory(mock(WebsocketClientSessionFactory.class))
                .build();

        TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        source.observable().subscribe(subscriber);
        subscriber.awaitValueCount(1, 1, TimeUnit.SECONDS);

        verify(converter).updateFrom(FEATURE_COLLECTION);
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
