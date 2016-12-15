package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.LayerStoreImpl;
import io.quartic.weyl.core.ObservableStore;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceDescriptor;
import io.quartic.weyl.core.source.SourceDescriptorImpl;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import org.junit.Test;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static rx.Observable.just;

public class SpatialJoinShould {
    private final UidGenerator<LayerId> lidGenerator = SequenceUidGenerator.of(LayerIdImpl::of);
    private final PublishSubject<SourceDescriptor> sources = PublishSubject.create();
    private final LayerStore store = LayerStoreImpl.builder()
            .entityStore(mock(ObservableStore.class))
            .sources(sources)
            .lidGenerator(lidGenerator)
            .build();

    @Test
    public void join_a_polygon_containing_a_point() throws Exception {
        NakedFeature polyA = square(0, 0, 0.1);
        NakedFeature polyB = square(1, 1, 0.1);
        NakedFeature pointA = point(0, 0);
        NakedFeature pointB = point(1, 1);
        Layer layerA = makeLayer(ImmutableList.of(polyA, polyB));
        Layer layerB = makeLayer(ImmutableList.of(pointA, pointB));

        List<Tuple> joinResults = new SpatialJoiner().innerJoin(layerA, layerB, SpatialJoiner.SpatialPredicate.CONTAINS)
                .collect(Collectors.toList());

        assertThat(joinResults, containsInAnyOrder(
                TupleImpl.of(feature(polyA, "1", "1"), feature(pointA, "2", "3")),
                TupleImpl.of(feature(polyB, "1", "2"), feature(pointB, "2", "4"))
        ));
    }

    private Layer makeLayer(Collection<NakedFeature> features) throws IOException {
        final LayerId layerId = lidGenerator.get();

        sources.onNext(SourceDescriptorImpl.of(
                layerId,
                LayerMetadataImpl.of("test", "test", Optional.empty(), Optional.empty()),
                IDENTITY_VIEW,
                AttributeSchemaImpl.builder().build(),
                true,
                just(SourceUpdateImpl.of(features))
        ));

        return store.getLayer(layerId).get();
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

    private Feature feature(NakedFeature feature, String layerId, String id) {
        return FeatureImpl.of(
                EntityIdImpl.of(layerId + "/" + feature.externalId().get()),
                feature.geometry(),
                feature.attributes()
        );
    }
}
