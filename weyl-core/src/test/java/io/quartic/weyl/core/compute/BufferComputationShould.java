package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BufferComputationShould {
    private BufferComputationImpl computation;
    private LayerId myLayerId = mock(LayerId.class);
    private LayerId sourceLayerId = mock(LayerId.class);
    private Layer layer = layer();

    @Before
    public void before() throws Exception {
        computation = BufferComputationImpl.builder()
                .layerId(myLayerId)
                .bufferSpec(BufferSpecImpl.of(sourceLayerId, 25.0))
                .clock(Clock.fixed(Instant.EPOCH, ZoneId.systemDefault()))
                .build();
    }


    @Test
    public void produce_valid_metadata() throws Exception {
        assertThat(computation.spec(newArrayList(layer)).metadata(), equalTo(LayerMetadataImpl.of(
                "Foo (buffered)",
                "Bar (buffered by 25.0m)",
                "Quartic",
                Instant.EPOCH
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
                entry(sourceLayerId, layer)
        );
    }

    private Layer layer() {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.spec().metadata().name()).thenReturn("Foo");
        when(layer.spec().metadata().description()).thenReturn("Bar");
        when(layer.spec().metadata().attribution()).thenReturn("Quartic");
        when(layer.features()).thenReturn(FeatureCollection.EMPTY_COLLECTION.append(
                ImmutableList.of(feature())
        ));
        return layer;
    }

    private Feature feature() {
        return FeatureImpl.of(EntityId.fromString("test"), point(), Attributes.EMPTY_ATTRIBUTES);
    }

    private Point point() {
        return new GeometryFactory().createPoint(new Coordinate(0, 0));
    }
}
