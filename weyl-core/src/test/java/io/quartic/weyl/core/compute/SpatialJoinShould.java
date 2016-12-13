package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.LayerReducer;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.transform;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class SpatialJoinShould {
    @Test
    public void join_a_polygon_containing_a_point() throws Exception {
        NakedFeature polyA = square(0, 0, 0.1);
        NakedFeature polyB = square(1, 1, 0.1);
        NakedFeature pointA = point(0, 0);
        NakedFeature pointB = point(1, 1);
        Layer layerA = makeLayer("1", ImmutableList.of(polyA, polyB));
        Layer layerB = makeLayer("2", ImmutableList.of(pointA, pointB));

        List<Tuple> joinResults = new SpatialJoiner().innerJoin(layerA, layerB, SpatialJoiner.SpatialPredicate.CONTAINS)
                .collect(toList());

        assertThat(joinResults, containsInAnyOrder(
                TupleImpl.of(feature(polyA, "1"), feature(pointA, "2")),
                TupleImpl.of(feature(polyB, "1"), feature(pointB, "2"))
        ));
    }

    private Layer makeLayer(String layerId, List<NakedFeature> features) throws IOException {
        final LayerSpec spec = LayerSpecImpl.of(
                LayerId.fromString(layerId),
                LayerMetadataImpl.of("test", "test", Optional.empty(), Optional.empty()),
                IDENTITY_VIEW,
                AttributeSchemaImpl.builder().build(),
                true
        );

        final LayerReducer reducer = new LayerReducer();
        return reducer.reduce(reducer.create(spec), transform(features, f -> feature(f, layerId)));
    }

    private NakedFeature point(double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry point = geometryFactory.createPoint(new Coordinate(x, y));
        return feature(point);
    }

    private NakedFeature square(double x, double y, double size) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(x - size, y - size),
                new Coordinate(x + size, y - size),
                new Coordinate(x + size, y + size),
                new Coordinate(x - size, y + size),
                new Coordinate(x - size, y - size)
        });
        return feature(geometry);
    }

    private NakedFeature feature(Geometry geometry) {
       return NakedFeatureImpl.of(Optional.of("123"), geometry, EMPTY_ATTRIBUTES);
    }

    private Feature feature(NakedFeature feature, String layerId) {
        return FeatureImpl.of(
                EntityIdImpl.of(layerId + "/" + feature.externalId().get()),
                feature.geometry(),
                feature.attributes()
        );
    }
}
