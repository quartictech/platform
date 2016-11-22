package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.compute.SpatialJoiner.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BucketComputationShould {
    private final Layer bucketLayer = bucketLayer();
    private final Layer featureLayer = featureLayer();
    private final LayerId bucketLayerId = mock(LayerId.class);
    private final LayerId featureLayerId = mock(LayerId.class);
    private final BucketAggregation aggregation = mock(BucketAggregation.class);
    private final BucketSpec spec = BucketSpecImpl.of(bucketLayerId, featureLayerId, aggregation, false);
    private final LayerStore store = mock(LayerStore.class);
    private final SpatialJoiner joiner = mock(SpatialJoiner.class);
    private BucketComputation computation;

    @Before
    public void before() throws Exception {
        when(store.getLayer(bucketLayerId)).thenReturn(Optional.of(bucketLayer));
        when(store.getLayer(featureLayerId)).thenReturn(Optional.of(featureLayer));

        computation = BucketComputationImpl.builder()
                .store(store)
                .bucketSpec(spec)
                .joiner(joiner)
                .build();
    }

    @Test
    public void generate_correct_layer_metadata_and_schema() throws Exception {
        final ComputationResults results = computation.compute().get();

        assertThat(results.metadata(), equalTo(LayerMetadataImpl.of(
                "Foo (bucketed)",
                "Foo bucketed by Bar aggregating by " + aggregation.toString(),
                Optional.empty(),
                Optional.empty()
        )));
        assertThat(results.schema(), equalTo(bucketSchema()
                .withPrimaryAttribute(name("Foo"))
                .withAttributes(ImmutableMap.<AttributeName, Attribute>builder()
                        .putAll(bucketLayer.schema().attributes())
                        .put(name("Foo"), AttributeImpl.of(NUMERIC, Optional.empty()))
                        .build()
                )
        ));
    }

    @Test
    public void generate_features_with_augmented_attributes() throws Exception {
        final FeatureImpl bucketFeature = bucketFeature();
        final ArrayList<Tuple> tuples = newArrayList(TupleImpl.of(bucketFeature, mock(Feature.class)));
        when(joiner.innerJoin(any(), any(), any())).thenReturn(tuples.stream());
        when(aggregation.aggregate(any(), any())).thenReturn(42.0);

        final ComputationResults results = computation.compute().get();

        assertThat(getLast(results.features()).attributes().attributes(),
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

        computation.compute().get();

        verify(joiner).innerJoin(bucketLayer, featureLayer, CONTAINS);
        verify(aggregation).aggregate(bucketFeature, newArrayList(groupFeatureA, groupFeatureB));
    }

    private Layer featureLayer() {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.metadata().name()).thenReturn("Foo");
        return layer;
    }

    private Layer bucketLayer() {
        final AttributeSchema schema = bucketSchema();

        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.metadata().name()).thenReturn("Bar");
        when(layer.schema()).thenReturn(schema);
        when(layer.indexedFeatures()).thenReturn(emptyList());
        return layer;
    }

    private AttributeSchemaImpl bucketSchema() {
        return AttributeSchemaImpl.of(
                    Optional.of(name("Title")),
                    Optional.of(name("Primary")),
                    Optional.of(name("Image")),
                    newArrayList(name("BlessedA"), name("BlessedB")),
                    ImmutableMap.of(name("WhateverC"), mock(Attribute.class), name("WhateverD"), mock(Attribute.class))
            );
    }

    private FeatureImpl bucketFeature() {
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
