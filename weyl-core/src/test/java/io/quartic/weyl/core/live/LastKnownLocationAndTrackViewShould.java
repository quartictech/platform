package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class LastKnownLocationAndTrackViewShould {
    @Test
    public void produce_point_for_a_single_feature() {
        AbstractFeature feature = locationFeature("alex", 100, 100);
        List<AbstractFeature> features = invokeView(ImmutableList.of(feature));

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_points_for_distinct_ids() {
        AbstractFeature featureA = locationFeature("alex", 100, 100);
        AbstractFeature featureB = locationFeature("bob", 100, 100);

        List<AbstractFeature> features = invokeView(ImmutableList.of(featureA, featureB));

        assertThat(features.size(), equalTo(2));
        assertThat(features, containsInAnyOrder(featureA, featureB));
    }

    @Test
    public void produce_lines_for_distinct_ids() {
        AbstractFeature featureA = locationFeature("bob", 200, 200);
        AbstractFeature featureB = locationFeature("alex", 300, 300);

        List<AbstractFeature> features = invokeView(ImmutableList.of(
                locationFeature("alex", 100, 100),
                locationFeature("bob", 100, 100),
                featureA,
                featureB));

        List<AbstractFeature> expectedFeatures = ImmutableList.of(
                lineFeature("bob", new Coordinate[]{
                        new Coordinate(100, 100),
                        new Coordinate(200, 200)
                }),
                lineFeature("alex", new Coordinate[]{
                        new Coordinate(100, 100),
                        new Coordinate(300, 300)
                }),
                featureA,
                featureB
        );

        assertThat(features.size(), equalTo(4));
        assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
    }

    @Test
    public void retain_input_order() {
        List<AbstractFeature> features = invokeView(ImmutableList.of(
                locationFeature("alex", 300, 300),
                locationFeature("alex", 100, 100),
                locationFeature("alex", 200, 200)
        ));

        assertThat(features, containsInAnyOrder(
                locationFeature("alex", 200, 200),
                lineFeature("alex", new Coordinate[]{
                        new Coordinate(300, 300),
                        new Coordinate(100, 100),
                        new Coordinate(200, 200)
                })
        ));
    }

    private List<AbstractFeature> invokeView(List<AbstractFeature> input) {
        return new LastKnownLocationAndTrackView()
                .compute(input)
                .collect(Collectors.toList());
    }

    private AbstractFeature locationFeature(String name, double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPoint(coordinate(x, y));
        return featureWithName(name, geometry);
    }

    private AbstractFeature lineFeature(String name, Coordinate[] coordinates) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createLineString(coordinates);
        return featureWithName(name, geometry);
    }

    private AbstractFeature featureWithName(String name, Geometry geometry) {
        return Feature.builder()
                .entityId(EntityId.of("foo/" + name))
                .geometry(geometry)
                .attributes(Attributes.builder().attribute(AttributeName.of("name"), name).build())
                .build();
    }

    private Coordinate coordinate(double x, double y) {
        return new Coordinate(x, y);
    }
}

