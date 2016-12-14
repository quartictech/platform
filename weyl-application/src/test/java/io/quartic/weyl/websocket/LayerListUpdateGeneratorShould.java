package io.quartic.weyl.websocket;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.websocket.message.LayerInfoImpl;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.LayerListUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayerListUpdateGeneratorShould {
    private final PublishSubject<LayerSnapshotSequence> sequences = PublishSubject.create();
    private final TestSubscriber<SocketMessage> sub = TestSubscriber.create();

    @Before
    public void before() throws Exception {
        sequences.compose(new LayerListUpdateGenerator()).subscribe(sub);
    }

    @Test
    public void send_messages_on_updates_to_layer() throws Exception {
        final BehaviorSubject<Snapshot> foo = registerSequence("foo");

        final Snapshot snapshotA = snapshot("a");
        final Snapshot snapshotB = snapshot("b");
        foo.onNext(snapshotA);
        foo.onNext(snapshotB);

        sequences.onCompleted();
        foo.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), contains(
                message(layerInfo(snapshotA)),
                message(layerInfo(snapshotB))
        ));
    }

    @Test
    public void send_messages_on_new_layers() throws Exception {
        final BehaviorSubject<Snapshot> foo = registerSequence("foo");

        final Snapshot snapshotA = snapshot("a");
        foo.onNext(snapshotA);

        final BehaviorSubject<Snapshot> bar = registerSequence("bar");

        final Snapshot snapshotB = snapshot("b");
        bar.onNext(snapshotB);

        sequences.onCompleted();
        foo.onCompleted();
        bar.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), contains(
                message(layerInfo(snapshotA)),
                message(layerInfo(snapshotA), layerInfo(snapshotB))
        ));
    }

    private BehaviorSubject<Snapshot> registerSequence(String id) {
        final BehaviorSubject<Snapshot> snapshots = BehaviorSubject.create();
        sequences.onNext(sequence(id, snapshots));
        return snapshots;
    }

    private LayerListUpdateMessage message(LayerInfo... infos) {
        return LayerListUpdateMessageImpl.of(asList(infos));
    }

    private LayerInfo layerInfo(Snapshot snapshot) {
        return LayerInfoImpl.of(
                snapshot.absolute().spec().id(),
                snapshot.absolute().spec().metadata(),
                snapshot.absolute().stats(),
                snapshot.absolute().spec().schema(),
                !snapshot.absolute().spec().indexable()
        );
    }

    private LayerSnapshotSequence sequence(String id, Observable<Snapshot> snapshots) {
        return LayerSnapshotSequenceImpl.of(LayerId.fromString(id), snapshots);
    }

    private Snapshot snapshot(String id) {
        final Snapshot snapshot = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(snapshot.absolute().spec().id()).thenReturn(LayerId.fromString(id));
        return snapshot;
    }
}
