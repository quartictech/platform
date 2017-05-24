package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.SnapshotReducer;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.SnapshotId;
import io.quartic.weyl.core.model.StaticSchema;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static io.quartic.common.uid.UidUtilsKt.sequenceGenerator;
import static io.quartic.weyl.api.LayerUpdateType.REPLACE;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
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

        List<Tuple> joinResults = new SpatialJoiner().innerJoin(layerA, layerB, SpatialPredicate.CONTAINS)
                .collect(toList());

        assertThat(joinResults, containsInAnyOrder(
                new Tuple(feature(polyA, "1"), feature(pointA, "2")),
                new Tuple(feature(polyB, "1"), feature(pointB, "2"))
        ));
    }

    private Layer makeLayer(String layerId, List<NakedFeature> features) throws IOException {
        final LayerSpec spec = new LayerSpec(
                new LayerId(layerId),
                new LayerMetadata("test", "test", "test", Instant.now()),
                IDENTITY_VIEW,
                new StaticSchema(),
                true
        );

        final SnapshotReducer reducer = new SnapshotReducer(sequenceGenerator(SnapshotId::new));
        return reducer.next(reducer.empty(spec), new LayerUpdate(REPLACE, features)).getAbsolute();
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
       return new NakedFeature("123", geometry, Attributes.Companion.getEMPTY_ATTRIBUTES());
    }

    private Feature feature(NakedFeature feature, String layerId) {
        return new Feature(
                new EntityId(layerId + "/" + feature.getExternalId()),
                feature.getGeometry(),
                feature.getAttributes()
        );
    }
}
