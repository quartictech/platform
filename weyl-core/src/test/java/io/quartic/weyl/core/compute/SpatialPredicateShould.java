package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.*;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SpatialPredicateShould {
    private SpatialPredicateComputationImpl computation;
    private LayerId myLayerId = mock(LayerId.class);
    private LayerId layerAId = mock(LayerId.class);
    private LayerId layerBId = mock(LayerId.class);
    private Layer layerA = layer("A");
    private Layer layerB = layer("B");

    @Before
    public void before() throws Exception {
        computation = SpatialPredicateComputationImpl.builder()
                .layerId(myLayerId)
                .spatialPredicateSpec(SpatialPredicateSpecImpl.
                        of(layerAId, layerBId, SpatialPredicate.CONTAINS))
                .clock(Clock.fixed(Instant.EPOCH, ZoneId.systemDefault()))
                .build();
    }


    @Test
    public void produce_valid_metadata() throws Exception {
        assertThat(computation.spec(newArrayList(layerA, layerB)).metadata(), equalTo(LayerMetadataImpl.of(
                "A CONTAINS B",
                "A CONTAINS B",
                "Quartic",
                Instant.EPOCH,
                Optional.empty()
        )));
    }

    @Test
    public void not_complete() throws Exception {
        TestSubscriber<LayerUpdate> subscriber = TestSubscriber.create();
        computation.updates(transform(computation.dependencies(), layerMap()::get))
                .subscribe(subscriber);

        TimeUnit.SECONDS.sleep(2);
        subscriber.assertNotCompleted();
    }

    private Map<LayerId, Layer> layerMap() {
        return map(
                entry(layerAId, layerA),
                entry(layerBId, layerB)
        );
    }

    private Layer layer(String name) {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.spec().metadata().name()).thenReturn(name);
        when(layer.spec().metadata().description()).thenReturn(name);
        when(layer.spec().metadata().attribution()).thenReturn("Quartic");
        Feature feature = feature();
        when(layer.features()).thenReturn(FeatureCollection.EMPTY_COLLECTION.append(
                ImmutableList.of(feature)
        ));
        when(layer.indexedFeatures()).thenReturn(ImmutableList.of(
                ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(prepare(feature.geometry()))
                        .build()
        ));
        return layer;
    }

    private PreparedGeometry prepare(Geometry geometry) {
        return new PreparedGeometryFactory().create(geometry);
    }

    private Feature feature() {
        return FeatureImpl.of(EntityId.fromString("test"), point(), Attributes.EMPTY_ATTRIBUTES);
    }

    private Point point() {
        return new GeometryFactory().createPoint(new Coordinate(0, 0));
    }
}
