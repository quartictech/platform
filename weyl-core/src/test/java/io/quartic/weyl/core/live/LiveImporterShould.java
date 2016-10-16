package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import org.junit.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class LiveImporterShould {
    public static final Instant TIMESTAMP = Instant.now();
    private final GeometryFactory factory = new GeometryFactory();

    // TODO: multiple LiveEvents

    @Test
    public void convert_features() throws Exception {
        final Collection<LiveEvent> events = liveEvents(geojsonFeature("a", Optional.of(point())));

        LiveImporter importer = importer(events);

        assertThat(importer.getFeatures(), equalTo(ImmutableList.of(
                io.quartic.weyl.core.model.ImmutableFeature.builder()
                        .uid(FeatureId.of("1"))
                        .externalId("a")
                        .geometry(factory.createPoint(new Coordinate(123.0, 456.0)))
                        .metadata(ImmutableMap.of("timestamp", 1234))
                        .build()
        )));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final Collection<LiveEvent> events = liveEvents(geojsonFeature("a", Optional.empty()));

        LiveImporter importer = importer(events);

        assertThat(importer.getFeatures(), empty());
    }

    @Test
    public void enrich_events() throws Exception {
        final FeedEvent feedEvent = FeedEvent.of("abc", "def", ImmutableMap.of("a", 999));
        final Collection<LiveEvent> events = liveEvents(feedEvent);

        LiveImporter importer = importer(events);

        assertThat(importer.getFeedEvents(), equalTo(ImmutableList.of(
                EnrichedFeedEvent.of(LiveEventId.of("1"), TIMESTAMP, feedEvent)
        )));
    }

    private LiveImporter importer(Collection<LiveEvent> events) {
        return new LiveImporter(events,
                new SequenceUidGenerator<>(FeatureId::of),
                new SequenceUidGenerator<>(LiveEventId::of)
        );
    }

    private Collection<LiveEvent> liveEvents(Feature... features) {
        return ImmutableList.of(LiveEvent.of(
                Instant.now(),
                Optional.of(FeatureCollection.of(newArrayList(features))),
                Optional.empty()));
    }

    private Collection<LiveEvent> liveEvents(FeedEvent feedEvent) {
        return ImmutableList.of(LiveEvent.of(
                TIMESTAMP,
                Optional.empty(),
                Optional.of(feedEvent)));
    }

    private Feature geojsonFeature(String id, Optional<Geometry> geometry) {
        return Feature.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private Point point() {
        return point(123.0, 456.0);
    }

    private Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }
}
