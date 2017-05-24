package io.quartic.weyl.core;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import io.quartic.common.test.rx.Interceptor;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable.Transformer;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;

@RunWith(HierarchicalContextRunner.class)
public class LayerRouterShould {
    private static final LayerId LAYER_ID = new LayerId("666");
    private static final LayerId OTHER_LAYER_ID = new LayerId("777");

    private final SnapshotReducer snapshotReducer = mock(SnapshotReducer.class);
    private final PublishSubject<LayerPopulator> populators = PublishSubject.create();
    private final Interceptor<LayerPopulator> interceptor = new Interceptor<>();
    private final LayerRouter router = new LayerRouter(populators.compose(interceptor), snapshotReducer);

    public class UpstreamConsumption {
        @Test
        public void consume_even_if_no_subscribers() throws Exception {
            final LayerSpec spec = spec(LAYER_ID);
            mockSnapshotCreationFor(spec);

            // Note no subscribers

            createLayer(spec);

            assertThat(interceptor.getValues(), hasSize(1));
        }

        @Test
        public void consume_updates_even_if_no_subscribers() throws Exception {
            final LayerSpec spec = spec(LAYER_ID);
            mockSnapshotCreationFor(spec);

            final Interceptor<LayerUpdate> interceptor = new Interceptor<>();
            createLayer(spec, interceptor).onNext(mock(LayerUpdate.class));

            // Note no subscribers

            assertThat(interceptor.getValues(), hasSize(1));
        }
    }

    public class SequencesObservableSemantics {
        private final LayerSpec specA = spec(LAYER_ID);
        private final LayerSpec specB = spec(OTHER_LAYER_ID);

        @Before
        public void before() throws Exception {
            mockSnapshotCreationFor(specA);
            mockSnapshotCreationFor(specB);
        }

        @Test
        public void emit_sequence_for_every_populator() throws Exception {
            final TestSubscriber<LayerSnapshotSequence> subscriber = subscribeToSequences();

            createLayer(specA);
            createLayer(specB);

            assertThat(transform(subscriber.getOnNextEvents(), LayerSnapshotSequence::getSpec), contains(specA, specB));
        }

        @Test
        public void emit_all_previous_sequences_on_subscription() throws Exception {
            createLayer(specA);
            createLayer(specB);

            final TestSubscriber<LayerSnapshotSequence> subscriber = subscribeToSequences();

            assertThat(transform(subscriber.getOnNextEvents(), LayerSnapshotSequence::getSpec), contains(specA, specB));
        }
    }

    public class SnapshotObservableSemantics {
        private final LayerSpec spec = spec(LAYER_ID);
        private final Snapshot empty = mockSnapshotCreationFor(spec);
        private final Snapshot updateA = mockSnapshotReductionFor(empty);
        private final Snapshot updateB = mockSnapshotReductionFor(updateA);

        @Test
        public void emit_initial_snapshot_on_layer_creation() throws Exception {
            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);

            createLayer(spec);

            assertThat(snapshotSubscriber.getOnNextEvents(), contains(empty));
        }

        @Test
        public void emit_empty_snapshot_and_complete_on_layer_completion() throws Exception {
            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);

            final PublishSubject<LayerUpdate> layer = createLayer(spec);
            layer.onCompleted();

            assertThat(snapshotSubscriber.getOnNextEvents(), contains(anySnapshot(), equalTo(empty)));
            assertCompleted(snapshotSubscriber);
        }

        @Test
        public void emit_empty_snapshot_and_complete_if_subscribed_after_source_completion() throws Exception {
            final PublishSubject<LayerUpdate> layer = createLayer(spec);
            layer.onCompleted();

            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);

            assertThat(snapshotSubscriber.getOnNextEvents(), contains(equalTo(empty)));
            assertCompleted(snapshotSubscriber);
        }

        @Test
        public void emit_latest_element_on_layer_subscription() throws Exception {
            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);

            final PublishSubject<LayerUpdate> layer = createLayer(spec);
            layer.onNext(mock(LayerUpdate.class));

            assertThat(getLast(snapshotSubscriber.getOnNextEvents()), equalTo(updateA));
        }

        @Test
        public void emit_snapshot_on_every_update() throws Exception {
            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);

            PublishSubject<LayerUpdate> updates = createLayer(spec);
            updates.onNext(mock(LayerUpdate.class));
            updates.onNext(mock(LayerUpdate.class));

            assertThat(snapshotSubscriber.getOnNextEvents(), contains(anySnapshot(), equalTo(updateA), equalTo(updateB)));
        }
    }

    public final class ErrorHandling {
        private final LayerSpec spec = spec(LAYER_ID);
        private final Snapshot empty = mockSnapshotCreationFor(spec);

        @Test
        public void not_throw_error_or_emit_sequence_if_layer_creation_fails() throws Exception {
            TestSubscriber<LayerSnapshotSequence> subscriber = subscribeToSequences();

            failWhenCreatingLayer();

            assertNoInteractions(subscriber);
        }

        @Test
        public void not_throw_error_or_emit_sequence_if_reducer_empty_fails() throws Exception {
            TestSubscriber<LayerSnapshotSequence> subscriber = subscribeToSequences();
            mockFailedSnapshotCreationFor(spec);

            createLayer(spec);

            assertNoInteractions(subscriber);
        }

        @Test
        public void complete_stream_if_reducer_next_fails() throws Exception {
            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);
            mockFailedSnapshotReductionFor(empty);

            PublishSubject<LayerUpdate> updates = createLayer(spec);
            updates.onNext(mock(LayerUpdate.class));

            assertThat(snapshotSubscriber.getOnNextEvents(), contains(anySnapshot(), equalTo(empty)));
            assertCompleted(snapshotSubscriber);
        }

        @Test
        public void complete_stream_on_nested_upstream_error() throws Exception {
            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(spec);

            PublishSubject<LayerUpdate> updates = createLayer(spec);
            updates.onError(exception());

            assertThat(snapshotSubscriber.getOnNextEvents(), contains(anySnapshot(), equalTo(empty)));
            assertCompleted(snapshotSubscriber);
        }

        private void failWhenCreatingLayer() {
            final LayerPopulator populator = mock(LayerPopulator.class);
            when(populator.dependencies()).thenThrow(exception());
            populators.onNext(populator);
        }

        private void mockFailedSnapshotCreationFor(LayerSpec spec) {
            when(snapshotReducer.empty(spec)).thenThrow(exception());
        }

        private void mockFailedSnapshotReductionFor(Snapshot snapshot) {
            when(snapshotReducer.next(eq(snapshot), any())).thenThrow(exception());
        }

        private RuntimeException exception() {
            return new RuntimeException("So sadness");
        }
    }

    public class DependencyManagement {
        private final LayerSpec specDependency = spec(LAYER_ID);
        private final LayerSpec specDependent = spec(OTHER_LAYER_ID);
        private final LayerPopulator populatorForDependent = mock(LayerPopulator.class);

        @Before
        public void before() throws Exception {
            when(populatorForDependent.dependencies()).thenReturn(singletonList(LAYER_ID)); // Specify another layer as a dependency
            when(populatorForDependent.spec(any())).thenReturn(specDependent);
            when(populatorForDependent.updates(any())).thenReturn(empty());
        }

        @Test
        public void prevent_overwriting_an_existing_layer() throws Exception {
            final LayerSpec spec = spec(LAYER_ID);
            mockSnapshotCreationFor(spec);

            createLayer(spec);
            createLayer(spec);

            verify(snapshotReducer, times(1)).empty(any());
        }

        @Test
        public void resolve_layer_dependencies_to_latest_snapshot() throws Exception {
            final Snapshot dependency = mockSnapshotReductionFor(mockSnapshotCreationFor(specDependency));
            mockSnapshotCreationFor(specDependent);
            createLayer(specDependency).onNext(mock(LayerUpdate.class));

            populators.onNext(populatorForDependent);

            verify(populatorForDependent).spec(singletonList(dependency.getAbsolute()));
            verify(populatorForDependent).updates(singletonList(dependency.getAbsolute()));
            verify(snapshotReducer).empty(specDependency);
            verify(snapshotReducer).empty(specDependent);
        }

        @Test
        public void complete_layer_if_dependencies_have_completed() throws Exception {
            mockSnapshotCreationFor(specDependency);
            mockSnapshotCreationFor(specDependent);
            createLayer(specDependency).onCompleted();

            TestSubscriber<Snapshot> snapshotSubscriber = subscribeToSnapshotsFor(specDependent);

            populators.onNext(populatorForDependent);

            assertCompleted(snapshotSubscriber);
        }
    }

    public final class ReducerManagement {
        @Test
        public void apply_creation_and_updates_via_reducer() throws Exception {
            final LayerSpec spec = spec(LAYER_ID);
            final Snapshot empty = mockSnapshotCreationFor(spec);
            mockSnapshotReductionFor(empty);

            final LayerUpdate update = mock(LayerUpdate.class);
            createLayer(spec).onNext(update);

            verify(snapshotReducer).empty(spec);
            verify(snapshotReducer).next(empty, update);
        }
    }

    private Snapshot mockSnapshotCreationFor(LayerSpec spec) {
        final Snapshot snapshot = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(snapshot.getAbsolute().getSpec()).thenReturn(spec);
        when(snapshotReducer.empty(spec)).thenReturn(snapshot);
        return snapshot;
    }

    private Snapshot mockSnapshotReductionFor(Snapshot snapshot) {
        final LayerSpec originalSpec = snapshot.getAbsolute().getSpec();
        final Snapshot next = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(next.getAbsolute().getSpec()).thenReturn(originalSpec);
        when(snapshotReducer.next(eq(snapshot), any())).thenReturn(next);
        return next;
    }

    private TestSubscriber<LayerSnapshotSequence> subscribeToSequences() {
        TestSubscriber<LayerSnapshotSequence> sub = TestSubscriber.create();
        router.snapshotSequences().subscribe(sub);
        return sub;
    }

    private TestSubscriber<Snapshot> subscribeToSnapshotsFor(LayerSpec spec) {
        TestSubscriber<Snapshot> sub = TestSubscriber.create();
        router.snapshotSequences()
                .first(s -> s.getSpec().equals(spec))
                .subscribe(s -> s.getSnapshots().subscribe(sub));
        return sub;
    }

    private PublishSubject<LayerUpdate> createLayer(LayerSpec spec) {
        return createLayer(spec, x -> x);
    }

    private PublishSubject<LayerUpdate> createLayer(LayerSpec spec, Transformer<LayerUpdate, LayerUpdate> transformer) {
        final PublishSubject<LayerUpdate> updates = PublishSubject.create();
        populators.onNext(LayerPopulator.withoutDependencies(spec, updates.compose(transformer)));
        return updates;
    }

    private LayerSpec spec(LayerId id) {
        final LayerSpec spec = mock(LayerSpec.class, RETURNS_DEEP_STUBS);
        when(spec.getId()).thenReturn(id);
        when(spec.getMetadata().getName()).thenReturn("foo");
        return spec;
    }

    private Matcher<Snapshot> anySnapshot() {
        return Matchers.any(Snapshot.class);
    }

    private void assertNoInteractions(TestSubscriber<LayerSnapshotSequence> subscriber) {
        assertThat(subscriber.getOnNextEvents(), hasSize(0));
        assertThat(subscriber.getOnErrorEvents(), hasSize(0));
        assertThat(subscriber.getCompletions(), equalTo(0));
    }

    private void assertCompleted(TestSubscriber<?> snapshotSubscriber) {
        assertThat(snapshotSubscriber.getCompletions(), equalTo(1));
    }
}
