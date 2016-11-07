package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Geometry;
import io.quartic.geojson.Point;
import io.quartic.terminator.api.FeatureCollectionWithDatasetId;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class TerminatorSourceFactoryShould {
    private final WebsocketListener listener = mock(WebsocketListener.class);
    private final LiveEventConverter converter = mock(LiveEventConverter.class);

    @Test
    public void import_things() throws Exception {
        final SourceUpdate update = SourceUpdate.of(newArrayList(), newArrayList());
        final FeatureCollection collection = featureCollection(geojsonFeature("a", Optional.of(point())));
        final DatasetId datasetId = DatasetId.of("123");

        when(listener.observable()).thenReturn(just(message(datasetId, collection)));
        when(converter.updateFrom(collection)).thenReturn(update);

        final TerminatorSourceFactory factory = createFactory();

        assertBehaviourForSource(update, collection, factory.sourceFor(datasetId));
    }

    @Test
    public void demultiplex_to_multiple_sources() throws Exception {
        final SourceUpdate updateA = SourceUpdate.of(newArrayList(), newArrayList());
        final SourceUpdate updateB = SourceUpdate.of(newArrayList(), newArrayList());
        final FeatureCollection collectionA = featureCollection(geojsonFeature("a", Optional.of(point())));
        final FeatureCollection collectionB = featureCollection(geojsonFeature("b", Optional.of(point())));
        final DatasetId datasetIdA = DatasetId.of("123");
        final DatasetId datasetIdB = DatasetId.of("456");

        when(listener.observable()).thenReturn(just(
                message(datasetIdA, collectionA),
                message(datasetIdB, collectionB)
        ));
        when(converter.updateFrom(collectionA)).thenReturn(updateA);
        when(converter.updateFrom(collectionB)).thenReturn(updateB);

        final TerminatorSourceFactory factory = createFactory();

        assertBehaviourForSource(updateA, collectionA, factory.sourceFor(datasetIdA));
        assertBehaviourForSource(updateB, collectionB, factory.sourceFor(datasetIdB));
    }

    private void assertBehaviourForSource(SourceUpdate update, FeatureCollection collection, Source source) {
        SourceUpdate result = collectUpdate(source);
        verify(converter).updateFrom(collection);
        assertThat(result, is(update));
    }

    private String message(DatasetId datasetId, FeatureCollection featureCollection) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(
                FeatureCollectionWithDatasetId.of(datasetId, featureCollection)
        );
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
                .objectMapper(OBJECT_MAPPER)
                .listener(listener)
                .url("whatever")
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();
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
