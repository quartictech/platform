package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MostRecentGeometryViewShould {
    private final UidGenerator<FeatureId> uidGen = mock(SequenceUidGenerator.class);

    @Before
    public void setUp() throws Exception {
        when(uidGen.get())
                .thenReturn(FeatureId.of("1001"))
                .thenReturn(FeatureId.of("1002"));
    }

    @Test
    public void produce_point_for_a_single_point_feature() {
        AbstractFeature feature = locationFeature("alex", 123, 100, 100);
        List<AbstractFeature> features = invokeView(ImmutableList.of(feature));

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_polygon_for_a_single_polygon_feature() {
        AbstractFeature feature = polygonFeature("alex", 123, new Coordinate[]{
                new Coordinate(1, 1),
                new Coordinate(1, 2),
                new Coordinate(2, 2),
                new Coordinate(2, 1),
                new Coordinate(1, 1)
        });
        List<AbstractFeature> features = invokeView(ImmutableList.of(feature));

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_most_recent_polygon_for_multiple_polygon_features() {
        List<AbstractFeature> features = Lists.newArrayList();
        for (int i = 0 ; i < 10 ; i++) {
            AbstractFeature feature = polygonFeature("alex", 123, new Coordinate[]{
                    new Coordinate(i + 1, i + 1),
                    new Coordinate(i + 1, i + 2),
                    new Coordinate(i + 2, i + 2),
                    new Coordinate(i + 2, i + 1),
                    new Coordinate(i + 1, i + 1)
            });
            features.add(feature);
        }
        AbstractFeature oliverCurrent = locationFeature("oliver", 456, 100, 100);
        AbstractFeature alexCurrent = Iterables.getLast(features);
        features.add(oliverCurrent);
        List<AbstractFeature> newFeatures = invokeView(features);

        assertThat(newFeatures.size(), equalTo(2));
        assertThat(newFeatures, containsInAnyOrder(alexCurrent, oliverCurrent));
    }

    private List<AbstractFeature> invokeView(List<AbstractFeature> input) {
        return new MostRecentGeometryView()
                .compute(uidGen, input)
                .collect(Collectors.toList());
    }

    private AbstractFeature locationFeature(String name, int uid, double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPoint(coordinate(x, y));
        return featureWithName(name, uid, geometry);
    }

    private AbstractFeature polygonFeature(String name, int uid, Coordinate[] coordinates) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPolygon(coordinates);
        return featureWithName(name, uid, geometry);
    }

    private AbstractFeature featureWithName(String name, int uid, Geometry geometry) {
        return Feature.builder()
                .entityId(EntityId.of(LayerId.of("blah"), name))
                .uid(FeatureId.of(String.valueOf(uid)))
                .geometry(geometry)
                .attributes(Attributes.builder().attribute(AttributeName.of("name"), name).build())
                .build();
    }

    private Coordinate coordinate(double x, double y) {
        return new Coordinate(x, y);
    }
}

