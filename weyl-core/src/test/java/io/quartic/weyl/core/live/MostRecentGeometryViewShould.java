package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class MostRecentGeometryViewShould {
    private static final Attributes ATTRIBUTES = mock(Attributes.class);

    @Test
    public void produce_point_for_a_single_point_feature() {
        Feature feature = locationFeature("alex", 100, 100);
        List<Feature> features = invokeView(ImmutableList.of(feature));

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_polygon_for_a_single_polygon_feature() {
        Feature feature = polygonFeature("alex", new Coordinate[]{
                new Coordinate(1, 1),
                new Coordinate(1, 2),
                new Coordinate(2, 2),
                new Coordinate(2, 1),
                new Coordinate(1, 1)
        });
        List<Feature> features = invokeView(ImmutableList.of(feature));

        assertThat(features.size(), equalTo(1));
        assertThat(Iterables.getOnlyElement(features), equalTo(feature));
    }

    @Test
    public void produce_most_recent_polygon_for_multiple_polygon_features() {
        List<Feature> features = Lists.newArrayList();
        for (int i = 0 ; i < 10 ; i++) {
            Feature feature = polygonFeature("alex", new Coordinate[]{
                    new Coordinate(i + 1, i + 1),
                    new Coordinate(i + 1, i + 2),
                    new Coordinate(i + 2, i + 2),
                    new Coordinate(i + 2, i + 1),
                    new Coordinate(i + 1, i + 1)
            });
            features.add(feature);
        }
        Feature oliverCurrent = locationFeature("oliver", 100, 100);
        Feature alexCurrent = features.get(0);
        features.add(oliverCurrent);
        List<Feature> newFeatures = invokeView(features);

        assertThat(newFeatures.size(), equalTo(2));
        assertThat(newFeatures, containsInAnyOrder(alexCurrent, oliverCurrent));
    }

    private List<Feature> invokeView(List<Feature> input) {
        return new MostRecentGeometryView()
                .compute(input)
                .collect(Collectors.toList());
    }

    private Feature locationFeature(String name, double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPoint(coordinate(x, y));
        return featureWithName(name, geometry);
    }

    private Feature polygonFeature(String name, Coordinate[] coordinates) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPolygon(coordinates);
        return featureWithName(name, geometry);
    }

    private Feature featureWithName(String name, Geometry geometry) {
        return new Feature(new EntityId("blah/" + name), geometry, ATTRIBUTES);
    }

    private Coordinate coordinate(double x, double y) {
        return new Coordinate(x, y);
    }
}

