package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableFeature;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

public class LastKnownLocationAndTrackViewShould {
    @Test
    public void produce_point_for_a_single_feature() {
        Feature feature = locationFeature("alex", 100, 100);
        List<Feature> features = new LastKnownLocationAndTrackView()
                .compute(ImmutableList.of(feature))
                .collect(Collectors.toList());

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_points_for_distinct_ids() {
        Feature featureA = locationFeature("alex", 100, 100);
        Feature featureB = locationFeature("bob", 100, 100);

        List<Feature> features = new LastKnownLocationAndTrackView()
                .compute(ImmutableList.of(featureA, featureB))
                .collect(Collectors.toList());

        assertThat(features.size(), equalTo(2));
        assertThat(features, containsInAnyOrder(featureA, featureB));
    }

    @Test
    public void produce_lines_for_distinct_ids() {
        Feature featureA = locationFeature("bob", 200, 200);
        Feature featureB = locationFeature("alex", 300, 300);
        List<Feature> input = ImmutableList.of(
                locationFeature("alex", 100, 100),
                locationFeature("bob", 100, 100),
                featureA,
                featureB);

        List<Feature> features = new LastKnownLocationAndTrackView()
                .compute(input)
                .collect(Collectors.toList());

        List<Feature> expectedFeatures = ImmutableList.of(
                lineFeature("alex", new Coordinate[]{
                        new Coordinate(100, 100),
                        new Coordinate(300, 300)
                }),
                lineFeature("bob", new Coordinate[]{
                        new Coordinate(100, 100),
                        new Coordinate(200, 200)
                }),
                featureA,
                featureB
        );

        assertThat(features.size(), equalTo(4));
        assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
    }

    private Feature featureWithName(String name, Geometry geometry) {
        return ImmutableFeature.builder()
                .id(name)
                .geometry(geometry)
                .metadata(ImmutableMap.of("name", Optional.of(name)))
                .build();
    }

    private Feature locationFeature(String name, double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPoint(coordinate(x, y));
        return featureWithName(name, geometry);
    }

    private Feature lineFeature(String name, Coordinate[] coordinates) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createLineString(coordinates);
        return featureWithName(name, geometry);
    }

    private Coordinate coordinate(double x, double y) {
        return new Coordinate(x, y);
    }
}

