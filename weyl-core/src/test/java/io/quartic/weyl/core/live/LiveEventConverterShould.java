package io.quartic.weyl.core.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.Geometry;
import io.quartic.geojson.Point;
import io.quartic.model.FeedEvent;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class LiveEventConverterShould {
    public static final Instant TIMESTAMP = Instant.now();
    private final GeometryFactory factory = new GeometryFactory();
    private final LiveEventConverter converter = new LiveEventConverter(
            new SequenceUidGenerator<>(FeatureId::of),
            new SequenceUidGenerator<>(LiveEventId::of),
            GeometryTransformer.webMercatorToWebMercator()
    );

    // TODO: multiple LiveEvents

    @Test
    public void convert_just_features() throws Exception {
        final FeatureCollection collection = featureCollectionOf(geojsonFeature("a", Optional.of(point())));

        final SourceUpdate update = converter.updateFrom(collection);

        assertThat(update.features(), equalTo(ImmutableList.of(
                io.quartic.weyl.core.model.ImmutableFeature.builder()
                        .uid(FeatureId.of("1"))
                        .externalId("a")
                        .geometry(factory.createPoint(new Coordinate(51.0, 0.1)))
                        .metadata(ImmutableMap.of("timestamp", 1234))
                        .build()
        )));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final FeatureCollection collection = featureCollectionOf(geojsonFeature("a", Optional.empty()));

        final SourceUpdate update = converter.updateFrom(collection);

        assertThat(update.features(), empty());
    }

    @Test
    public void enrich_events() throws Exception {
        final FeedEvent feedEvent = FeedEvent.of("abc", "def", ImmutableMap.of("a", 999));
        final LiveEvent event = liveEvent(feedEvent);

        final SourceUpdate update = converter.updateFrom(event);

        assertThat(update.feedEvents(), equalTo(ImmutableList.of(
                EnrichedFeedEvent.of(LiveEventId.of("1"), TIMESTAMP, feedEvent)
        )));
    }

    private FeatureCollection featureCollectionOf(Feature... features) {
        return FeatureCollection.of(newArrayList(features));
    }

    private LiveEvent liveEvent(FeedEvent feedEvent) {
        return LiveEvent.of(
                TIMESTAMP,
                Optional.empty(),
                Optional.of(feedEvent));
    }

    private Feature geojsonFeature(String id, Optional<Geometry> geometry) {
        return Feature.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private Point point() {
        return point(51.0, 0.1);
    }

    private Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }
}
