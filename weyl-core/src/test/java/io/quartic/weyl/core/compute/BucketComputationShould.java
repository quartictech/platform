package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.StaticSchema;
import io.quartic.weyl.core.model.StaticSchemaImpl;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.rx.RxUtilsKt.all;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static io.quartic.weyl.core.compute.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BucketComputationShould {
    private final LayerId myLayerId = mock(LayerId.class);
    private final Layer bucketLayer = bucketLayer();
    private final Layer featureLayer = featureLayer();
    private final LayerId bucketLayerId = mock(LayerId.class);
    private final LayerId featureLayerId = mock(LayerId.class);
    private final BucketAggregation aggregation = mock(BucketAggregation.class);
    private final BucketSpec bucketSpec = BucketSpecImpl.of(bucketLayerId, featureLayerId, aggregation, false);
    private final SpatialJoiner joiner = mock(SpatialJoiner.class);
    private BucketComputation computation;

    @Before
    public void before() throws Exception {
        computation = BucketComputationImpl.builder()
                .layerId(myLayerId)
                .bucketSpec(bucketSpec)
                .joiner(joiner)
                .clock(Clock.fixed(Instant.EPOCH, ZoneId.systemDefault()))
                .build();
    }

    @Test
    public void generate_correct_layer_metadata_and_schema() throws Exception {
        final LayerSpec spec = evaluateSpec();

        assertThat(spec, equalTo(LayerSpecImpl.of(
                myLayerId,
                LayerMetadataImpl.of(
                        "Foo (bucketed)",
                        "Foo bucketed by Bar aggregating by " + aggregation.describe(),
                        "Alice / Bob",
                        Instant.EPOCH,
                        Optional.empty()
                ),
                IDENTITY_VIEW,
                StaticSchemaImpl.copyOf(bucketSchema())
                        .withPrimaryAttribute(name("Foo"))
                        .withBlessedAttributes(name("Foo"), name("BlessedA"), name("BlessedB")),
                true
        )));
    }

    @Test
    public void generate_features_with_augmented_attributes() throws Exception {
        final Feature bucketFeature = bucketFeature();
        final ArrayList<Tuple> tuples = newArrayList(TupleImpl.of(bucketFeature, mock(Feature.class)));
        when(joiner.innerJoin(any(), any(), any())).thenReturn(tuples.stream());
        when(aggregation.aggregate(any(), any())).thenReturn(42.0);

        assertThat(getLast(evaluateFirstUpdate().get(0).features()).attributes().attributes(),
                equalTo(ImmutableMap.builder()
                        .putAll(bucketFeature.attributes().attributes())
                        .put(name("Foo"), 42.0)
                        .build()
                )
        );
    }

    @Test
    public void wire_up_joiner_and_aggregation() throws Exception {
        final Feature bucketFeature = bucketFeature();
        final Feature groupFeatureA = mock(Feature.class);
        final Feature groupFeatureB = mock(Feature.class);
        final List<Tuple> tuples = newArrayList(
                TupleImpl.of(bucketFeature, groupFeatureA),
                TupleImpl.of(bucketFeature, groupFeatureB)
        );
        when(joiner.innerJoin(any(), any(), any())).thenReturn(tuples.stream());

        evaluateFirstUpdate();

        verify(joiner).innerJoin(bucketLayer, featureLayer, CONTAINS);
        verify(aggregation).aggregate(bucketFeature, newArrayList(groupFeatureA, groupFeatureB));
    }

    @Test
    public void not_complete() throws Exception {
        final Feature bucketFeature = bucketFeature();
        final Feature groupFeatureA = mock(Feature.class);
        final Feature groupFeatureB = mock(Feature.class);
        final List<Tuple> tuples = newArrayList(
                TupleImpl.of(bucketFeature, groupFeatureA),
                TupleImpl.of(bucketFeature, groupFeatureB)
        );
        when(joiner.innerJoin(any(), any(), any())).thenReturn(tuples.stream());

        TestSubscriber<LayerUpdate> subscriber = TestSubscriber.create();

        computation.updates(transform(computation.dependencies(), layerMap()::get))
                .subscribe(subscriber);
        TimeUnit.SECONDS.sleep(2);
        subscriber.assertNotCompleted();
    }

    private LayerSpec evaluateSpec() {
        return computation.spec(transform(computation.dependencies(), layerMap()::get));
    }

    private List<LayerUpdate> evaluateFirstUpdate() {
        return all(computation.updates(transform(computation.dependencies(), layerMap()::get))
                .take(1));
    }

    private Map<LayerId, Layer> layerMap() {
        return map(
                entry(featureLayerId, featureLayer),
                entry(bucketLayerId, bucketLayer)
        );
    }

    private Layer featureLayer() {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.spec().metadata().name()).thenReturn("Foo");
        when(layer.spec().metadata().attribution()).thenReturn("Alice");
        return layer;
    }

    private Layer bucketLayer() {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.spec().metadata().name()).thenReturn("Bar");
        when(layer.spec().metadata().attribution()).thenReturn("Bob");
        when(layer.spec().staticSchema()).thenReturn(bucketSchema());
        when(layer.indexedFeatures()).thenReturn(emptyList());
        return layer;
    }

    private StaticSchema bucketSchema() {
        return StaticSchemaImpl.of(
                Optional.of(name("Title")),
                Optional.of(name("Primary")),
                Optional.of(name("Image")),
                newHashSet(name("BlessedA"), name("BlessedB")),
                emptySet(),
                ImmutableMap.of()
        );
    }

    private Feature bucketFeature() {
        return FeatureImpl.of(
                EntityId.fromString("12345"),
                mock(Geometry.class),
                () -> ImmutableMap.of(name("Height"), 180, name("Weight"), 70)
        );
    }

    private AttributeName name(String name) {
        return AttributeNameImpl.of(name);
    }
}
