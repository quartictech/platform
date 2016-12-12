package io.quartic.weyl.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import org.junit.Test;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class LayerReducerShould {

    private static final LayerId LAYER_ID = LayerId.fromString("666");

    private final LayerReducer reducer = new LayerReducer();

    @Test
    public void preserve_core_schema_info_upon_update() throws Exception {
        Layer original = reducer.create(spec(LAYER_ID, true));
        Layer updated = reducer.reduce(original, newArrayList(feature("a")));

        assertThat(updated.spec().schema().blessedAttributes(), contains(AttributeNameImpl.of("blah")));
    }

    @Test
    public void calculate_indices_for_indexable_layer() throws Exception {
        assertThatLayerIndexedFeaturesHasSize(true, 1);
    }

    @Test
    public void not_calculate_indices_for_non_indexable_layer() throws Exception {
        assertThatLayerIndexedFeaturesHasSize(false, 0);
    }

    private void assertThatLayerIndexedFeaturesHasSize(boolean indexable, int size) {
        Layer original = reducer.create(spec(LAYER_ID, indexable));
        Layer updated = reducer.reduce(original, newArrayList(feature("a")));

        assertThat(updated.indexedFeatures(), hasSize(size));
    }

    private static LayerSpec spec(LayerId layerId, boolean indexable) {
        return LayerSpecImpl.of(
                layerId,
                metadata("foo", "bar"),
                IDENTITY_VIEW,
                schema("blah"),
                indexable
        );
    }

    private static AttributeSchema schema(String blessed) {
        return AttributeSchemaImpl.builder().blessedAttribute(AttributeNameImpl.of(blessed)).build();
    }

    private static LayerMetadata metadata(String name, String description) {
        return LayerMetadataImpl.of(name, description, Optional.empty(), Optional.empty());
    }

    private Feature feature(String externalId) {
        return feature(LAYER_ID.uid(), externalId);
    }

    private Feature feature(String layerId, String externalId) {
        return FeatureImpl.of(
                EntityIdImpl.of(layerId + "/" + externalId),
                new GeometryFactory().createPoint(new Coordinate(123.0, 456.0)),
                EMPTY_ATTRIBUTES
        );
    }
}
