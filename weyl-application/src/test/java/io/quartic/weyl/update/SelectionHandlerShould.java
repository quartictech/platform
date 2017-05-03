package io.quartic.weyl.update;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.SelectionStatus;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SelectionHandlerShould {
    @SuppressWarnings("unchecked")
    private final SelectionDrivenUpdateGenerator generator = mock(SelectionDrivenUpdateGenerator.class);
    private final PublishSubject<LayerSnapshotSequence> sequences = PublishSubject.create();
    private final SelectionHandler handler = new SelectionHandler(sequences, newArrayList(generator));

    private final PublishSubject<ClientStatusMessage> statuses = PublishSubject.create();
    private final TestSubscriber<SocketMessage> sub = TestSubscriber.create();

    private final LayerId layerIdX = mock(LayerId.class);
    private final LayerId layerIdY = mock(LayerId.class);
    private final EntityId entityIdA = mock(EntityId.class);
    private final EntityId entityIdB = mock(EntityId.class);
    private final Feature featureA = feature(entityIdA);
    private final Feature featureB = feature(entityIdB);

    @Before
    public void before() throws Exception {
        when(generator.name()).thenReturn("Donkey");
        when(generator.generate(any())).thenReturn(mock(Object.class));
    }

    @Test
    public void create_message_as_combination_of_generator_output_and_seq_num() throws Exception {
        final Object payload = mock(Object.class);
        when(generator.generate(any())).thenReturn(payload);

        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);

        sequence.onNext(snapshot(featureA));
        statuses.onNext(status(42, entityIdA));

        assertThat(sub.getOnNextEvents(), contains(new SelectionDrivenUpdateMessage("Donkey", 42, payload)));
    }

    @Test
    public void lookup_latest_entity_value() throws Exception {
        final Feature featureAPrime = feature(entityIdA);

        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA));
        sequence.onNext(snapshot(featureAPrime));
        statuses.onNext(status(42, entityIdA));

        assertGeneratorCalledWith(featureAPrime);
    }

    @Test
    public void lookup_latest_entity_value_even_if_subscribed_later() throws Exception {
        final Feature featureAPrime = feature(entityIdA);

        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA));
        sequence.onNext(snapshot(featureAPrime));
        subscribe();                                // Much later
        statuses.onNext(status(42, entityIdA));

        assertGeneratorCalledWith(featureAPrime);
    }

    @Test
    public void lookup_multiple_entities() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA, featureB));
        statuses.onNext(status(42, entityIdA, entityIdB));

        assertGeneratorCalledWith(featureA, featureB);
    }

    @Test
    public void lookup_entities_from_multiple_layers() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequenceX = createSequence(layerIdX);
        final PublishSubject<Snapshot> sequenceY = createSequence(layerIdY);
        sequenceX.onNext(snapshot(featureA));
        sequenceY.onNext(snapshot(featureB));
        statuses.onNext(status(42, entityIdA, entityIdB));

        assertGeneratorCalledWith(featureA, featureB);
    }

    @Test
    public void pass_only_entities_that_are_wanted() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA, featureB));      // Two features
        statuses.onNext(status(42, entityIdA));             // But only one desired

        assertGeneratorCalledWith(featureA);
    }

    @Test
    public void pass_only_entities_that_can_be_found() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA));                // No featureB
        statuses.onNext(status(42, entityIdA, entityIdB));  // But we're looking for both!

        assertGeneratorCalledWith(featureA);                // That's ok though
    }

    @Test
    public void respond_to_status_changes_affecting_selection() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA, featureB));
        statuses.onNext(status(42, entityIdA));
        statuses.onNext(status(43, entityIdB));

        assertGeneratorCallCountIs(2);
    }

    @Test
    public void ignore_status_changes_not_affecting_selection() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        sequence.onNext(snapshot(featureA, featureB));
        statuses.onNext(status(42, entityIdA));
        statuses.onNext(status(42, entityIdA)); // Same

        assertGeneratorCallCountIs(1);
    }

    @Test
    public void respond_to_relevant_entity_changes() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        statuses.onNext(status(42, entityIdA));
        sequence.onNext(snapshot(feature(entityIdA)));
        sequence.onNext(snapshot(feature(entityIdA)));  // Different feature with the same entityID

        assertGeneratorCallCountIs(2);
    }

    @Test
    public void ignore_irrelevant_entity_changes() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        statuses.onNext(status(42, entityIdA));
        sequence.onNext(snapshot(featureA));
        sequence.onNext(snapshot(featureB));  // Unrelated feature

        assertGeneratorCallCountIs(1);
    }

    @Test
    public void perform_cheap_equality_checks() throws Exception {
        final Feature feature = new AlwaysUnequalFeature(entityIdA);
        assertThat(feature, not(equalTo(feature)));     // Sanity check

        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        statuses.onNext(status(42, entityIdA));
        sequence.onNext(snapshot(feature));
        sequence.onNext(snapshot(feature));             // Same feature, but compares unequal

        assertGeneratorCallCountIs(1);                  // Thus an identity check is required
    }

    @Test
    public void stop_passing_entities_that_came_from_deleted_layer() throws Exception {
        subscribe();
        final PublishSubject<Snapshot> sequence = createSequence(layerIdX);
        statuses.onNext(status(42, entityIdA));
        sequence.onNext(snapshot(featureA));
        sequence.onCompleted();                 // Layer deletion

        assertGeneratorCalledWith(featureA);
        assertGeneratorCalledWith();
    }

    private void subscribe() {
        statuses.compose(handler).subscribe(sub);
    }

    private PublishSubject<Snapshot> createSequence(LayerId layerId) {
        final PublishSubject<Snapshot> snapshots = PublishSubject.create();
        sequences.onNext(sequence(layerId, snapshots));
        return snapshots;
    }

    private LayerSnapshotSequence sequence(LayerId layerId, Observable<Snapshot> snapshots) {
        final LayerSnapshotSequence seq = mock(LayerSnapshotSequence.class, RETURNS_DEEP_STUBS);
        when(seq.spec().id()).thenReturn(layerId);
        when(seq.snapshots()).thenReturn(snapshots);
        return seq;
    }

    private Snapshot snapshot(Feature... features) {
        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.diff()).thenReturn(asList(features));
        return snapshot;
    }

    private ClientStatusMessage status(int seqNum, EntityId... entityIds) {
        final ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.getSelection()).thenReturn(new SelectionStatus(seqNum, asList(entityIds)));
        return msg;
    }

    private Feature feature(EntityId id) {
        final Feature feature = mock(Feature.class);
        when(feature.entityId()).thenReturn(id);
        return feature;
    }

    private void assertGeneratorCalledWith(Feature... features) {
        verify(generator).generate(asList(features));
    }

    private void assertGeneratorCallCountIs(int expected) {
        verify(generator, times(expected)).generate(any());
    }

    private static class AlwaysUnequalFeature implements Feature {
        private final EntityId entityId;

        private AlwaysUnequalFeature(EntityId entityId) {
            this.entityId = entityId;
        }

        @Override
        public EntityId entityId() {
            return entityId;
        }

        @Override
        public Geometry geometry() {
            return null;
        }

        @Override
        public Attributes attributes() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
}
