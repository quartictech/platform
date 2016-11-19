package io.quartic.weyl.core;

import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityStoreShould {

    private final EntityStore store = new EntityStore();

    @Test
    public void emit_entity_changes() throws Exception {
        final EntityId id = EntityId.of("123");
        final AbstractFeature featureA = feature(id);
        final AbstractFeature featureB = feature(id);
        final TestSubscriber<AbstractFeature> sub = TestSubscriber.create();

        store.get(id).subscribe(sub);
        store.putAll(newArrayList(featureA));
        store.putAll(newArrayList(featureB));
        sub.awaitValueCount(2, 100, MILLISECONDS);

        assertThat(sub.getOnNextEvents(), contains(featureA, featureB));
    }

    @Test
    public void emit_entities_for_different_ids() throws Exception {
        final EntityId idA = EntityId.of("123");
        final EntityId idB = EntityId.of("456");
        final AbstractFeature featureA = feature(idA);
        final AbstractFeature featureB = feature(idB);
        final TestSubscriber<AbstractFeature> subA = TestSubscriber.create();
        final TestSubscriber<AbstractFeature> subB = TestSubscriber.create();

        store.get(idA).subscribe(subA);
        store.get(idB).subscribe(subB);
        store.putAll(newArrayList(featureA));
        store.putAll(newArrayList(featureB));
        subA.awaitValueCount(1, 100, MILLISECONDS);
        subB.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(subA.getOnNextEvents(), contains(featureA));
        assertThat(subB.getOnNextEvents(), contains(featureB));
    }

    @Test
    public void emit_latest_entity_changes_on_subscription() throws Exception {
        final EntityId id = EntityId.of("123");
        final AbstractFeature feature = feature(id);
        final TestSubscriber<AbstractFeature> sub = TestSubscriber.create();

        store.putAll(newArrayList(feature));
        store.get(id).subscribe(sub);
        sub.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(sub.getOnNextEvents(), contains(feature));
    }

    private AbstractFeature feature(EntityId id) {
        final AbstractFeature feature = mock(AbstractFeature.class);
        when(feature.entityId()).thenReturn(id);
        return feature;
    }
}
