package io.quartic.weyl.core;

import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
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
        final EntityId id = EntityIdImpl.of("123");
        final Feature featureA = feature(id);
        final Feature featureB = feature(id);
        final TestSubscriber<Feature> sub = TestSubscriber.create();

        store.get(id).subscribe(sub);
        store.putAll(newArrayList(featureA));
        store.putAll(newArrayList(featureB));
        sub.awaitValueCount(2, 100, MILLISECONDS);

        assertThat(sub.getOnNextEvents(), contains(featureA, featureB));
    }

    @Test
    public void emit_entities_for_different_ids() throws Exception {
        final EntityId idA = EntityIdImpl.of("123");
        final EntityId idB = EntityIdImpl.of("456");
        final Feature featureA = feature(idA);
        final Feature featureB = feature(idB);
        final TestSubscriber<Feature> subA = TestSubscriber.create();
        final TestSubscriber<Feature> subB = TestSubscriber.create();

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
        final EntityId id = EntityIdImpl.of("123");
        final Feature feature = feature(id);
        final TestSubscriber<Feature> sub = TestSubscriber.create();

        store.putAll(newArrayList(feature));
        store.get(id).subscribe(sub);
        sub.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(sub.getOnNextEvents(), contains(feature));
    }

    private Feature feature(EntityId id) {
        final Feature feature = mock(Feature.class);
        when(feature.entityId()).thenReturn(id);
        return feature;
    }
}
