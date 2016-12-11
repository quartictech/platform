package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.LayerSpec;
import io.quartic.weyl.core.LayerSpecImpl;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static io.quartic.common.test.CollectionUtils.entry;
import static io.quartic.common.test.CollectionUtils.map;
import static io.quartic.common.test.rx.RxUtils.all;
import static io.quartic.weyl.core.compute.SpatialJoiner.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.never;

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
                .build();
    }

    @Test
    public void generate_correct_layer_metadata_and_schema() throws Exception {
        final LayerSpec spec = computeSpec();

        assertThat(spec, equalTo(LayerSpecImpl.of(
                myLayerId,
                LayerMetadataImpl.of(
                        "Foo (bucketed)",
                        "Foo bucketed by Bar aggregating by " + aggregation.toString(),
                        Optional.empty(),
                        Optional.empty()
                ),
                IDENTITY_VIEW,
                bucketSchema()
                        .withPrimaryAttribute(name("Foo"))
                        .withBlessedAttributes(name("Foo"), name("BlessedA"), name("BlessedB"))
                        .withAttributes(ImmutableMap.<AttributeName, Attribute>builder()
                                .putAll(bucketLayer.schema().attributes())
                                .put(name("Foo"), AttributeImpl.of(NUMERIC, Optional.empty()))
                                .build()
                        ),
                true,
                never() // Don't care
        )));
    }

    @Test
    public void generate_features_with_augmented_attributes() throws Exception {
        final FeatureImpl bucketFeature = bucketFeature();
        final ArrayList<Tuple> tuples = newArrayList(TupleImpl.of(bucketFeature, mock(Feature.class)));
        when(joiner.innerJoin(any(), any(), any())).thenReturn(tuples.stream());
        when(aggregation.aggregate(any(), any())).thenReturn(42.0);

        final LayerSpec spec = computeSpec();

        assertThat(getLast(all(spec.updates()).get(0).features()).attributes().attributes(),
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

        computeSpec();

        verify(joiner).innerJoin(bucketLayer, featureLayer, CONTAINS);
        verify(aggregation).aggregate(bucketFeature, newArrayList(groupFeatureA, groupFeatureB));
    }

    private LayerSpec computeSpec() {
        final Map<LayerId, Layer> layers = map(
                entry(featureLayerId, featureLayer),
                entry(bucketLayerId, bucketLayer)
        );
        return computation.spec(transform(computation.dependencies(), layers::get));
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
