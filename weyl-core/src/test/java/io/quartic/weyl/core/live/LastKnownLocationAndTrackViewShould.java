package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LastKnownLocationAndTrackViewShould {
    private final UidGenerator<FeatureId> uidGen = mock(SequenceUidGenerator.class);

    @Before
    public void setUp() throws Exception {
        when(uidGen.get())
                .thenReturn(FeatureId.of("1001"))
                .thenReturn(FeatureId.of("1002"));
    }

    @Test
    public void produce_point_for_a_single_feature() {
        Feature feature = locationFeature("alex", 123, 100, 100);
        List<Feature> features = invokeView(ImmutableList.of(feature));

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_points_for_distinct_ids() {
        Feature featureA = locationFeature("alex", 123, 100, 100);
        Feature featureB = locationFeature("bob", 456, 100, 100);

        List<Feature> features = invokeView(ImmutableList.of(featureA, featureB));

        assertThat(features.size(), equalTo(2));
        assertThat(features, containsInAnyOrder(featureA, featureB));
    }

    @Test
    public void produce_lines_for_distinct_ids() {
        Feature featureA = locationFeature("bob", 455, 200, 200);
        Feature featureB = locationFeature("alex", 123, 300, 300);

        List<Feature> features = invokeView(ImmutableList.of(
                locationFeature("alex", 122, 100, 100),
                locationFeature("bob", 454, 100, 100),
                featureA,
                featureB));

        List<Feature> expectedFeatures = ImmutableList.of(
                lineFeature("alex", 1001, new Coordinate[]{
                        new Coordinate(100, 100),
                        new Coordinate(300, 300)
                }),
                lineFeature("bob", 1002, new Coordinate[]{
                        new Coordinate(100, 100),
                        new Coordinate(200, 200)
                }),
                featureA,
                featureB
        );

        assertThat(features.size(), equalTo(4));
        assertThat(features, containsInAnyOrder(expectedFeatures.toArray()));
    }

    @Test
    public void produce_line_ordered_by_uid() {
        List<Feature> features = invokeView(ImmutableList.of(
                locationFeature("alex", 123, 100, 100),
                locationFeature("alex", 124, 200, 200),
                locationFeature("alex", 122, 300, 300)
        ));

        assertThat(features, containsInAnyOrder(
                locationFeature("alex", 124, 200, 200),
                lineFeature("alex", 1001, new Coordinate[]{
                        new Coordinate(300, 300),
                        new Coordinate(100, 100),
                        new Coordinate(200, 200)
                })
        ));
    }

    private List<Feature> invokeView(List<Feature> input) {
        return new LastKnownLocationAndTrackView()
                .compute(uidGen, input)
                .collect(Collectors.toList());
    }

    private Feature locationFeature(String name, int uid, double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPoint(coordinate(x, y));
        return featureWithName(name, uid, geometry);
    }

    private Feature lineFeature(String name, int uid, Coordinate[] coordinates) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createLineString(coordinates);
        return featureWithName(name, uid, geometry);
    }

    private Feature featureWithName(String name, int uid, Geometry geometry) {
        return ImmutableFeature.builder()
                .externalId(name)
                .uid(FeatureId.of(String.valueOf(uid)))
                .geometry(geometry)
                .metadata(ImmutableMap.of(AttributeName.of("name"), name))
                .build();
    }

    private Coordinate coordinate(double x, double y) {
        return new Coordinate(x, y);
    }
}

