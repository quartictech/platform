package io.quartic.weyl.core;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import static com.google.common.collect.Lists.transform;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;

public class LayerRouterShould {
    private static final LayerId LAYER_ID = LayerId.fromString("666");
    private static final LayerId OTHER_LAYER_ID = LayerId.fromString("777");

    private final SnapshotReducer snapshotReducer = mock(SnapshotReducer.class);
    private final PublishSubject<LayerPopulator> populators = PublishSubject.create();
    private final LayerRouter router = LayerRouterImpl.builder()
            .populators(populators)
            .snapshotReducer(snapshotReducer)
            .build();

    @Test
    public void prevent_overwriting_an_existing_layer() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockSnapshotCreationFor(spec);

        createLayer(spec);
        createLayer(spec);

        verify(snapshotReducer, times(1)).create(any());
    }

    @Test
    public void resolve_layer_dependencies_to_latest_snapshot() throws Exception {
        final LayerSpec specDependency = spec(LAYER_ID);
        final LayerSpec specDependent = spec(OTHER_LAYER_ID);
        final Snapshot dependency = mockSnapshotReductionFor(mockSnapshotCreationFor(specDependency));
        mockSnapshotCreationFor(specDependent);

        createLayer(specDependency).onNext(mock(LayerUpdate.class));

        final LayerPopulator populator = mock(LayerPopulator.class);
        when(populator.dependencies()).thenReturn(singletonList(LAYER_ID)); // Specify another layer as a dependency
        when(populator.spec(any())).thenReturn(specDependent);
        when(populator.updates(any())).thenReturn(empty());
        populators.onNext(populator);

        verify(populator).spec(singletonList(dependency.absolute()));
        verify(populator).updates(singletonList(dependency.absolute()));
        verify(snapshotReducer).create(specDependency);
        verify(snapshotReducer).create(specDependent);
    }

    @Test
    public void prevent_layer_creation_if_dependencies_have_completed() throws Exception {
        final LayerSpec specDependency = spec(LAYER_ID);
        final LayerSpec specDependent = spec(OTHER_LAYER_ID);
        mockSnapshotCreationFor(specDependency);
        mockSnapshotCreationFor(specDependent);

        createLayer(specDependency).onCompleted();

        final LayerPopulator populator = mock(LayerPopulator.class);
        when(populator.dependencies()).thenReturn(singletonList(LAYER_ID)); // Specify another layer as a dependency
        populators.onNext(populator);

        verify(snapshotReducer, times(1)).create(specDependency);
        verify(snapshotReducer, never()).create(specDependent);
    }

    @Test
    public void not_prevent_layer_creation_if_non_dependencies_have_completed() throws Exception {
        final LayerSpec specA = spec(LAYER_ID);
        final LayerSpec specB = spec(OTHER_LAYER_ID);
        mockSnapshotCreationFor(specA);
        mockSnapshotCreationFor(specB);

        createLayer(specA).onCompleted();
        createLayer(specB);

        verify(snapshotReducer).create(specA);
        verify(snapshotReducer).create(specB);
    }

    @Test
    public void apply_updates_via_reducer() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockSnapshotReductionFor(mockSnapshotCreationFor(spec));

        final LayerUpdate update = mock(LayerUpdate.class);
        createLayer(spec).onNext(update);

        verify(snapshotReducer).next(any(), eq(update));
    }

    @Test
    public void emit_sequence_every_time_layer_is_created() throws Exception {
        final LayerSpec specA = spec(LAYER_ID);
        final LayerSpec specB = spec(OTHER_LAYER_ID);
        mockSnapshotCreationFor(specA);
        mockSnapshotCreationFor(specB);

        TestSubscriber<LayerSnapshotSequence> sub = TestSubscriber.create();
        router.snapshotSequences().subscribe(sub);

        createLayer(specA);
        createLayer(specB);

        assertThat(transform(sub.getOnNextEvents(), LayerSnapshotSequence::spec), contains(specA, specB));
    }

    @Test
    public void emit_snapshot_every_time_layer_is_updated() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        final Snapshot original = mockSnapshotCreationFor(spec);
        final Snapshot update1 = mockSnapshotReductionFor(original);
        final Snapshot update2 = mockSnapshotReductionFor(update1);

        TestSubscriber<Snapshot> sub = TestSubscriber.create();
        router.snapshotSequences().subscribe(s -> s.snapshots().subscribe(sub)); // Subscribe to the nested snapshot observable

        PublishSubject<LayerUpdate> updates = createLayer(spec);
        updates.onNext(mock(LayerUpdate.class));
        updates.onNext(mock(LayerUpdate.class));

        assertThat(sub.getOnNextEvents(), contains(original, update1, update2));
    }

    private Snapshot mockSnapshotCreationFor(LayerSpec spec) {
        final Snapshot snapshot = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(snapshot.absolute().spec()).thenReturn(spec);
        when(snapshotReducer.create(spec)).thenReturn(snapshot);
        return snapshot;
    }

    private Snapshot mockSnapshotReductionFor(Snapshot snapshot) {
        final LayerSpec originalSpec = snapshot.absolute().spec();
        final Snapshot next = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(next.absolute().spec()).thenReturn(originalSpec);
        when(snapshotReducer.next(eq(snapshot), any())).thenReturn(next);
        return next;
    }

    private PublishSubject<LayerUpdate> createLayer(LayerSpec spec) {
        final PublishSubject<LayerUpdate> updates = PublishSubject.create();
        populators.onNext(LayerPopulator.withoutDependencies(spec, updates));
        return updates;
    }

    private LayerSpec spec(LayerId id) {
        final LayerSpec spec = mock(LayerSpec.class, RETURNS_DEEP_STUBS);
        when(spec.id()).thenReturn(id);
        when(spec.metadata().name()).thenReturn("foo");
        return spec;
    }
}
