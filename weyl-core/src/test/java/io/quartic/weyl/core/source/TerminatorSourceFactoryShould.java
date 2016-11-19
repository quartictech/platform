package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.TerminationIdImpl;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.catalogue.api.TerminatorDatasetLocatorImpl;
import io.quartic.common.client.WebsocketListener;
import io.quartic.geojson.*;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.terminator.api.FeatureCollectionWithTerminationIdImpl;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class TerminatorSourceFactoryShould {
    private final WebsocketListener<FeatureCollectionWithTerminationId> listener = mock(WebsocketListener.class);
    private final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
    private final LiveEventConverter converter = mock(LiveEventConverter.class);

    @Before
    public void before() throws Exception {
        when(listenerFactory.create(FeatureCollectionWithTerminationId.class)).thenReturn(listener);
    }

    @Test
    public void import_things() throws Exception {
        final SourceUpdate update = SourceUpdateImpl.of(newArrayList());
        final FeatureCollection collection = featureCollection(geojsonFeature("a", Optional.of(point())));
        final TerminatorDatasetLocator locator = TerminatorDatasetLocatorImpl.of(TerminationIdImpl.of("123"));

        when(listener.observable()).thenReturn(just(
                FeatureCollectionWithTerminationIdImpl.of(locator.id(), collection)
        ));
        when(converter.updateFrom(collection)).thenReturn(update);

        final TerminatorSourceFactory factory = createFactory();

        assertBehaviourForSource(update, collection, factory.sourceFor(locator));
    }

    @Test
    public void demultiplex_to_multiple_sources() throws Exception {
        final SourceUpdate updateA = SourceUpdateImpl.of(newArrayList());
        final SourceUpdate updateB = SourceUpdateImpl.of(newArrayList());
        final FeatureCollection collectionA = featureCollection(geojsonFeature("a", Optional.of(point())));
        final FeatureCollection collectionB = featureCollection(geojsonFeature("b", Optional.of(point())));
        final TerminatorDatasetLocator locatorA = TerminatorDatasetLocatorImpl.of(TerminationIdImpl.of("123"));
        final TerminatorDatasetLocator locatorB = TerminatorDatasetLocatorImpl.of(TerminationIdImpl.of("456"));

        when(listener.observable()).thenReturn(just(
                FeatureCollectionWithTerminationIdImpl.of(locatorA.id(), collectionA),
                FeatureCollectionWithTerminationIdImpl.of(locatorB.id(), collectionB)
        ));
        when(converter.updateFrom(collectionA)).thenReturn(updateA);
        when(converter.updateFrom(collectionB)).thenReturn(updateB);

        final TerminatorSourceFactory factory = createFactory();

        assertBehaviourForSource(updateA, collectionA, factory.sourceFor(locatorA));
        assertBehaviourForSource(updateB, collectionB, factory.sourceFor(locatorB));
    }

    private void assertBehaviourForSource(SourceUpdate update, FeatureCollection collection, Source source) {
        SourceUpdate result = collectUpdate(source);
        verify(converter).updateFrom(collection);
        assertThat(result, is(update));
    }

    private SourceUpdate collectUpdate(Source source) {
        TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();
        source.observable().subscribe(subscriber);
        subscriber.awaitValueCount(1, 1, TimeUnit.SECONDS);
        return subscriber.getOnNextEvents().get(0);
    }

    private TerminatorSourceFactory createFactory() {
        return TerminatorSourceFactory.builder()
                .converter(converter)
                .listenerFactory(listenerFactory)
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();
    }

    // TODO: there's a lot of duplication of helper methods here (with e.g. LiveEventConverterShould)

    private static FeatureCollection featureCollection(Feature... features) {
        return FeatureCollectionImpl.of(newArrayList(features));
    }

    private static Feature geojsonFeature(String id, Optional<Geometry> geometry) {
        return FeatureImpl.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private static Point point() {
        return point(51.0, 0.1);
    }

    private static Point point(double x, double y) {
        return PointImpl.of(ImmutableList.of(x, y));
    }
}
