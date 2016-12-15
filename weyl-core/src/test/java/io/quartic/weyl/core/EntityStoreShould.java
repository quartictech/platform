package io.quartic.weyl.core;

import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.SnapshotImpl;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static rx.Observable.from;

public class EntityStoreShould {

    private final PublishSubject<LayerSnapshotSequence> sequences = PublishSubject.create();
    private final EntityStore store = new EntityStore(sequences);

    @Test
    public void emit_changes_after_subscribed() throws Exception {
        final EntityId id = mock(EntityId.class);
        final Feature featureA = feature(id);
        final Feature featureB = feature(id);

        final TestSubscriber<Feature> sub = subscriberFor(store, id);
        sequences.onNext(sequence(
                newArrayList(featureA),
                newArrayList(featureB)
        ));

        sub.awaitValueCount(2, 100, MILLISECONDS);
        assertThat(sub.getOnNextEvents(), contains(featureA, featureB));
    }

    @Test
    public void emit_changes_for_different_ids() throws Exception {
        final EntityId idA = mock(EntityId.class);
        final EntityId idB = mock(EntityId.class);
        final Feature featureA = feature(idA);
        final Feature featureB = feature(idB);

        final TestSubscriber<Feature> subA = subscriberFor(store, idA);
        final TestSubscriber<Feature> subB = subscriberFor(store, idB);
        sequences.onNext(sequence(
                newArrayList(featureA),
                newArrayList(featureB)
        ));

        subA.awaitValueCount(1, 100, MILLISECONDS);
        subB.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subA.getOnNextEvents(), contains(featureA));
        assertThat(subB.getOnNextEvents(), contains(featureB));
    }

    @Test
    public void aggregate_changes_across_all_sequences() throws Exception {
        final EntityId idA = mock(EntityId.class);
        final EntityId idB = mock(EntityId.class);
        final Feature featureA = feature(idA);
        final Feature featureB = feature(idB);

        final TestSubscriber<Feature> subA = subscriberFor(store, idA);
        final TestSubscriber<Feature> subB = subscriberFor(store, idB);
        sequences.onNext(sequence(newArrayList(featureA)));
        sequences.onNext(sequence(newArrayList(featureB)));

        subA.awaitValueCount(1, 100, MILLISECONDS);
        subB.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subA.getOnNextEvents(), contains(featureA));
        assertThat(subB.getOnNextEvents(), contains(featureB));
    }

    @Test
    public void emit_current_feature_value_on_subscription() throws Exception {
        final EntityId id = mock(EntityId.class);
        final Feature feature = feature(id);

        sequences.onNext(sequence(newArrayList(feature)));  // Before subscription

        final TestSubscriber<Feature> sub = subscriberFor(store, id);

        sub.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(sub.getOnNextEvents(), contains(feature));
    }

    private TestSubscriber<Feature> subscriberFor(EntityStore store, EntityId id) {
        final TestSubscriber<Feature> sub = TestSubscriber.create();
        store.get(id).subscribe(sub);
        return sub;
    }

    @SafeVarargs
    private final LayerSnapshotSequence sequence(List<Feature>... features) {
        final LayerSnapshotSequence sequence = mock(LayerSnapshotSequence.class);
        when(sequence.snapshots()).thenReturn(from(features).map(f -> SnapshotImpl.of(mock(Layer.class), f)));
        return sequence;
    }

    private Feature feature(EntityId id) {
        final Feature feature = mock(Feature.class);
        when(feature.entityId()).thenReturn(id);
        return feature;
    }
}
