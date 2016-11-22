package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BucketComputationShould {

    @Test
    public void generate_correct_layer_metadata_and_schema() throws Exception {
        final Layer bucketLayer = createBucketLayer();
        final Layer featureLayer = createFeatureLayer();

        final LayerId bucketLayerId = mock(LayerId.class);
        final LayerId featureLayerId = mock(LayerId.class);
        final BucketAggregation aggregation = mock(BucketAggregation.class);
        final BucketSpec spec = BucketSpecImpl.of(bucketLayerId, featureLayerId, aggregation, false);

        final LayerStore store = mock(LayerStore.class);
        when(store.getLayer(bucketLayerId)).thenReturn(Optional.of(bucketLayer));
        when(store.getLayer(featureLayerId)).thenReturn(Optional.of(featureLayer));

        final BucketComputation computation = BucketComputation.create(store, spec);

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

    private Layer createFeatureLayer() {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.metadata().name()).thenReturn("Foo");
        return layer;
    }

    private Layer createBucketLayer() {
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

    private AttributeName name(String name) {
        return AttributeNameImpl.of(name);
    }
}
