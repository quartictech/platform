package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.model.*;
import org.hamcrest.Matchers;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpatialJoinShould {
    private final UidGenerator<FeatureId> fidGenerator = SequenceUidGenerator.of(FeatureId::of);
    private final UidGenerator<LayerId> lidGenerator = SequenceUidGenerator.of(LayerId::of);
    private final FeatureStore featureStore = new FeatureStore(fidGenerator);
    private final LayerStore store = new LayerStore(featureStore, lidGenerator);

    @Test
    public void join_a_polygon_containing_a_point() throws Exception {
        Feature polyA = square(0, 0, 0.1);
        Feature polyB = square(1, 1, 0.1);
        Feature pointA = point(0, 0);
        Feature pointB = point(1, 1);
        AbstractLayer layerA = makeLayer(ImmutableList.of(polyA, polyB));
        AbstractLayer layerB = makeLayer(ImmutableList.of(pointA, pointB));

        List<Tuple> joinResults = SpatialJoin.innerJoin(layerA, layerB, SpatialJoin.SpatialPredicate.CONTAINS)
                .collect(Collectors.toList());

        assertThat(joinResults, Matchers.containsInAnyOrder(
                Tuple.of(polyA, pointA),
                Tuple.of(polyB, pointB)
        ));
    }

    private AbstractLayer makeLayer(Collection<Feature> features) throws IOException {
        final LayerId layerId = lidGenerator.get();
        final Subscriber<SourceUpdate> subscriber = store.createLayer(layerId, LayerMetadata.of("test", "test", Optional.empty(), Optional.empty()), true);

        Observable.just(SourceUpdate.of(features, emptyList()))
                .subscribe(subscriber);

        return store.getLayer(layerId).get();
    }

    private Feature point(double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry point = geometryFactory.createPoint(new Coordinate(x, y));
        return feature(point);
    }

    private Feature square(double x, double y, double size) {
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

    private Feature feature(Geometry geometry) {
       return ImmutableFeature.of("123", FeatureId.of("123"), geometry, ImmutableMap.of());
    }
}
