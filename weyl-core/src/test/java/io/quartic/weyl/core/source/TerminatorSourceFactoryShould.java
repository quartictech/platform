package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.catalogue.api.TerminatorDatasetLocator;
import io.quartic.catalogue.api.TerminatorDatasetLocatorImpl;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Geometry;
import io.quartic.common.geojson.Point;
import io.quartic.common.websocket.WebsocketListener;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class TerminatorSourceFactoryShould {
    private final WebsocketListener<FeatureCollectionWithTerminationId> listener = mock(WebsocketListener.class);
    private final WebsocketListener.Factory listenerFactory = mock(WebsocketListener.Factory.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);

    @Before
    public void before() throws Exception {
        when(listenerFactory.create(FeatureCollectionWithTerminationId.class)).thenReturn(listener);
    }

    @Test
    public void import_things() throws Exception {
        final Collection<NakedFeature> modelFeatures = mock(Collection.class);
        final FeatureCollection collection = featureCollection(geojsonFeature("a", point()));
        final TerminatorDatasetLocator locator = TerminatorDatasetLocatorImpl.of(new TerminationId("123"));

        when(listener.getObservable()).thenReturn(just(
                new FeatureCollectionWithTerminationId(locator.id(), collection)
        ));
        when(converter.toModel(collection)).thenReturn(modelFeatures);

        final TerminatorSourceFactory factory = createFactory();

        assertBehaviourForSource(modelFeatures, collection, factory.sourceFor(locator, converter));
    }

    @Test
    public void demultiplex_to_multiple_sources() throws Exception {
        final Collection<NakedFeature> modelFeaturesA = mock(Collection.class);
        final Collection<NakedFeature> modelFeaturesB = mock(Collection.class);
        final FeatureCollection collectionA = featureCollection(geojsonFeature("a", point()));
        final FeatureCollection collectionB = featureCollection(geojsonFeature("b", point()));
        final TerminatorDatasetLocator locatorA = TerminatorDatasetLocatorImpl.of(new TerminationId("123"));
        final TerminatorDatasetLocator locatorB = TerminatorDatasetLocatorImpl.of(new TerminationId("456"));

        when(listener.getObservable()).thenReturn(just(
                new FeatureCollectionWithTerminationId(locatorA.id(), collectionA),
                new FeatureCollectionWithTerminationId(locatorB.id(), collectionB)
        ));
        when(converter.toModel(collectionA)).thenReturn(modelFeaturesA);
        when(converter.toModel(collectionB)).thenReturn(modelFeaturesB);

        final TerminatorSourceFactory factory = createFactory();

        assertBehaviourForSource(modelFeaturesA, collectionA, factory.sourceFor(locatorA, converter));
        assertBehaviourForSource(modelFeaturesB, collectionB, factory.sourceFor(locatorB, converter));
    }

    private void assertBehaviourForSource(Collection<NakedFeature> modelFeatures, FeatureCollection collection, Source source) {
        LayerUpdate result = collectUpdate(source);
        verify(converter).toModel(collection);
        assertThat(result, equalTo(LayerUpdateImpl.of(modelFeatures)));
    }

    private LayerUpdate collectUpdate(Source source) {
        TestSubscriber<LayerUpdate> subscriber = TestSubscriber.create();
        source.observable().subscribe(subscriber);
        subscriber.awaitValueCount(1, 1, TimeUnit.SECONDS);
        return subscriber.getOnNextEvents().get(0);
    }

    private TerminatorSourceFactory createFactory() {
        return TerminatorSourceFactory.builder()
                .listenerFactory(listenerFactory)
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();
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
