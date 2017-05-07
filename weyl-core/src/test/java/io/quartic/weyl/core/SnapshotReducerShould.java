package io.quartic.weyl.core;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.api.LayerUpdateType;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.DynamicSchemaKt;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.SnapshotId;
import io.quartic.weyl.core.model.StaticSchema;
import org.junit.Test;

import java.time.Instant;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.uid.UidUtilsKt.sequenceGenerator;
import static io.quartic.weyl.api.LayerUpdateType.*;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SnapshotReducerShould {

    private static final LayerId LAYER_ID = new LayerId("666");

    private final UidGenerator<SnapshotId> sidGen = sequenceGenerator(SnapshotId::new);
    private final SnapshotReducer reducer = new SnapshotReducer(sidGen);

    @Test
    public void provide_unique_ids() throws Exception {
        final Snapshot initial = initialSnapshot();
        final Snapshot updated = reducer.next(initial, updateFor(APPEND));

        assertThat(updated.getId(), not(equalTo(initial.getId())));
    }

    @Test
    public void provide_update_features_as_diff() throws Exception {
        Snapshot updated = reducer.next(initialSnapshot(), updateFor(APPEND, nakedFeature("a")));

        assertThat(updated.getDiff().getUpdateType(), equalTo(APPEND));
        assertThat(updated.getDiff().getFeatures(), contains(feature(LAYER_ID + "/a")));
    }

    @Test
    public void provide_external_id_if_not_present() throws Exception {
        Snapshot updated = reducer.next(initialSnapshot(), updateFor(APPEND,
                nakedFeature(null), nakedFeature(null)));

        assertThat(updated.getDiff().getFeatures(), contains(feature(LAYER_ID + "/1"), feature(LAYER_ID + "/2")));
    }

    @Test
    public void propagate_replace_update_type() {
        Snapshot updated = reducer.next(initialSnapshot(), updateFor(REPLACE, nakedFeature("a")));

        assertThat(updated.getDiff().getUpdateType(), equalTo(REPLACE));
    }

    @Test
    public void preserve_core_schema_info_upon_update() throws Exception {
        Snapshot original = initialSnapshot();
        Snapshot updated = reducer.next(original, updateFor(APPEND, nakedFeature("a")));

        assertThat(updated.getAbsolute().getSpec().getStaticSchema().getBlessedAttributes(), contains(new AttributeName("blah")));
    }

    @Test
    public void handle_basic_replace() throws Exception {
        Snapshot snapshot1 = reducer.next(initialSnapshot(), updateFor(APPEND, nakedFeature("a")));
        Snapshot snapshot2 = reducer.next(snapshot1, updateFor(REPLACE, nakedFeature("b")));

        assertThat(snapshot2.getDiff().getFeatures(), contains(feature(LAYER_ID + "/b")));
        assertThat(snapshot2.getAbsolute().getFeatures(), contains(feature(LAYER_ID + "/b")));
        assertThat(snapshot2.getAbsolute().getFeatures(), not(contains(feature(LAYER_ID + "/a"))));
    }

    @Test
    public void handle_replace_then_append() throws Exception {
        Snapshot snapshot1 = reducer.next(initialSnapshot(), updateFor(APPEND, nakedFeature("a")));
        Snapshot snapshot2 = reducer.next(snapshot1, updateFor(REPLACE, nakedFeature("b")));
        Snapshot snapshot3 = reducer.next(snapshot2, updateFor(APPEND, nakedFeature("c")));

        assertThat(snapshot3.getDiff().getFeatures(), contains(feature(LAYER_ID + "/c")));
        assertThat(snapshot3.getAbsolute().getFeatures(), containsInAnyOrder(feature(LAYER_ID + "/b"), feature(LAYER_ID + "/c")));
        assertThat(snapshot3.getAbsolute().getFeatures(), not(contains(feature(LAYER_ID + "/a"))));
    }

    @Test
    public void clear_dynamic_schema_on_replace() {
        Snapshot snapshot = initialSnapshot();

        // TODO: Kotlin-ify the grossness
        Layer layerWithDynamicSchema = new Layer(
                snapshot.getAbsolute().getSpec(),
                snapshot.getAbsolute().getFeatures(),
                new DynamicSchema(ImmutableMap.of(
                        new AttributeName("wat"), new Attribute(AttributeType.NUMERIC, null)
                )),
                snapshot.getAbsolute().getSpatialIndex(),
                snapshot.getAbsolute().getIndexedFeatures(),
                snapshot.getAbsolute().getStats()
        );
        // TODO: Kotlin-ify the grossness
        Snapshot snapshot1 = new Snapshot(
                snapshot.getId(),
                layerWithDynamicSchema,
                snapshot.getDiff()
        );
        assertThat(snapshot1.getAbsolute().getDynamicSchema(), not(equalTo(DynamicSchemaKt.getEMPTY_SCHEMA())));
        Snapshot snapshot2 = reducer.next(snapshot1, updateFor(REPLACE, nakedFeature("a")));
        assertThat(snapshot2.getAbsolute().getDynamicSchema(), equalTo(DynamicSchemaKt.getEMPTY_SCHEMA()));
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
        Snapshot updated = reducer.next(original, updateFor(APPEND, nakedFeature("a")));

        assertThat(updated.getAbsolute().getIndexedFeatures(), hasSize(size));
    }

    private Snapshot initialSnapshot() {
        return reducer.empty(spec(LAYER_ID, true));
    }

    private static LayerSpec spec(LayerId layerId, boolean indexable) {
        return new LayerSpec(
                layerId,
                metadata("foo", "bar"),
                IDENTITY_VIEW,
                schema("blah"),
                indexable
        );
    }

    private static StaticSchema schema(String blessed) {
        final StaticSchema schema = mock(StaticSchema.class);
        when(schema.getBlessedAttributes()).thenReturn(newArrayList(new AttributeName(blessed)));
        return schema;
    }

    private static LayerMetadata metadata(String name, String description) {
        return new LayerMetadata(name, description, "", Instant.now());
    }

    private LayerUpdate updateFor(LayerUpdateType updateType, NakedFeature... features) {
        return new LayerUpdate(updateType, asList(features));
    }

    private NakedFeature nakedFeature(String externalId) {
        return new NakedFeature(
                externalId,
                new GeometryFactory().createPoint(new Coordinate(123.0, 456.0)),
                EMPTY_ATTRIBUTES
        );
    }

    private Feature feature(String entityId) {
        return new Feature(
                new EntityId(entityId),
                new GeometryFactory().createPoint(new Coordinate(123.0, 456.0)),
                EMPTY_ATTRIBUTES
        );
    }
}
