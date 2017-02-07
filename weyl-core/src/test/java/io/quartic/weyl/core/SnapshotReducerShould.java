package io.quartic.weyl.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.model.StaticSchema;
import io.quartic.weyl.core.model.StaticSchemaImpl;
import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class SnapshotReducerShould {

    private static final LayerId LAYER_ID = LayerId.fromString("666");

    private final SnapshotReducer reducer = new SnapshotReducer();

    @Test
    public void provide_update_features_as_diff() throws Exception {
        Snapshot updated = reducer.next(initialSnapshot(), updateFor(LayerUpdate.Type.APPEND,
                nakedFeature(Optional.of("a"))));

        assertThat(updated.diff(), contains(feature(LAYER_ID + "/a")));
    }

    @Test
    public void provide_external_id_if_not_present() throws Exception {
        Snapshot updated = reducer.next(initialSnapshot(), updateFor(LayerUpdate.Type.APPEND,
                nakedFeature(Optional.empty()), nakedFeature(Optional.empty())));

        assertThat(updated.diff(), contains(feature(LAYER_ID + "/1"), feature(LAYER_ID + "/2")));
    }

    @Test
    public void preserve_core_schema_info_upon_update() throws Exception {
        Snapshot original = initialSnapshot();
        Snapshot updated = reducer.next(original, updateFor(LayerUpdate.Type.APPEND,
                nakedFeature(Optional.of("a"))));

        assertThat(updated.absolute().spec().staticSchema().blessedAttributes(), contains(AttributeNameImpl.of("blah")));
    }

    @Test
    public void handle_basic_replace() throws Exception {
        Snapshot snapshot1 = reducer.next(initialSnapshot(),
                updateFor(LayerUpdate.Type.APPEND,
                        nakedFeature(Optional.of("a"))));

        Snapshot snapshot2 = reducer.next(snapshot1,
                updateFor(LayerUpdate.Type.REPLACE,
                        nakedFeature(Optional.of("b"))));
        assertThat(snapshot2.diff(), contains(feature(LAYER_ID + "/b")));
        assertThat(snapshot2.absolute().features(), contains(feature(LAYER_ID + "/b")));
        assertThat(snapshot2.absolute().features(), not(contains(feature(LAYER_ID + "/a"))));
    }

    @Test
    public void handle_replace_then_append() throws Exception {
        Snapshot snapshot1 = reducer.next(initialSnapshot(),
                updateFor(LayerUpdate.Type.APPEND,
                        nakedFeature(Optional.of("a"))));
        Snapshot snapshot2 = reducer.next(snapshot1,
                updateFor(LayerUpdate.Type.REPLACE,
                        nakedFeature(Optional.of("b"))));
         Snapshot snapshot3 = reducer.next(snapshot2,
                updateFor(LayerUpdate.Type.APPEND,
                        nakedFeature(Optional.of("c"))));

        assertThat(snapshot3.diff(), contains(feature(LAYER_ID + "/c")));
        assertThat(snapshot3.absolute().features(), containsInAnyOrder(feature(LAYER_ID + "/b"), feature(LAYER_ID + "/c")));
        assertThat(snapshot3.absolute().features(), not(contains(feature(LAYER_ID + "/a"))));
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
        Snapshot original = reducer.empty(spec(LAYER_ID, indexable));
        Snapshot updated = reducer.next(original, updateFor(LayerUpdate.Type.APPEND,
                nakedFeature(Optional.of("a"))));

        assertThat(updated.absolute().indexedFeatures(), hasSize(size));
    }

    private Snapshot initialSnapshot() {
        return reducer.empty(spec(LAYER_ID, true));
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

    private static StaticSchema schema(String blessed) {
        return StaticSchemaImpl.builder()
                .blessedAttribute(AttributeNameImpl.of(blessed))
                .build();
    }

    private static LayerMetadata metadata(String name, String description) {
        return LayerMetadataImpl.of(name, description, "", Instant.now(), Optional.empty());
    }

    private LayerUpdate updateFor(LayerUpdate.Type updateType, NakedFeature... features) {
        return LayerUpdateImpl.of(updateType, asList(features));
    }

    private NakedFeature nakedFeature(Optional<String> externalId) {
        return NakedFeatureImpl.of(
                externalId,
                new GeometryFactory().createPoint(new Coordinate(123.0, 456.0)),
                EMPTY_ATTRIBUTES
        );
    }

    private Feature feature(String entityId) {
        return FeatureImpl.of(
                EntityId.fromString(entityId),
                new GeometryFactory().createPoint(new Coordinate(123.0, 456.0)),
                EMPTY_ATTRIBUTES
        );
    }
}
