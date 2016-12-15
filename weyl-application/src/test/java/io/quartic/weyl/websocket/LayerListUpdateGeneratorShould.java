package io.quartic.weyl.websocket;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.websocket.message.LayerInfoImpl;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.LayerListUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
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
    public void send_messages_on_new_layers() throws Exception {
        final LayerSpec specFoo = spec("foo");
        final LayerSpec specBar = spec("bar");
        final BehaviorSubject<Snapshot> foo = registerSequence(specFoo);
        final BehaviorSubject<Snapshot> bar = registerSequence(specBar);

        sequences.onCompleted();
        foo.onCompleted();
        bar.onCompleted();

        sub.awaitTerminalEvent();
        for (SocketMessage socketMessage : sub.getOnNextEvents()) {
            System.out.println("YYY: " + socketMessage);
        }
        assertThat(sub.getOnNextEvents(), contains(
                message(),
                message(layerInfo(specFoo)),
                message(layerInfo(specFoo), layerInfo(specBar))
        ));
    }

    @Test
    public void not_send_messages_on_updates_to_layer() throws Exception {
        final LayerSpec specFoo = spec("foo");
        final BehaviorSubject<Snapshot> foo = registerSequence(specFoo);

        foo.onNext(snapshot("a"));
        sequences.onCompleted();
        foo.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), contains(
                message(),
                message(layerInfo(specFoo)) // And no more messages even though there are snapshots
        ));
    }

    private BehaviorSubject<Snapshot> registerSequence(LayerSpec spec) {
        final BehaviorSubject<Snapshot> snapshots = BehaviorSubject.create();
        sequences.onNext(LayerSnapshotSequenceImpl.of(spec, snapshots));
        return snapshots;
    }

    private LayerListUpdateMessage message(LayerInfo... infos) {
        return LayerListUpdateMessageImpl.of(asList(infos));
    }

    private LayerInfo layerInfo(LayerSpec spec) {
        return LayerInfoImpl.of(
                spec.id(),
                spec.metadata(),
                spec.staticSchema(),
                !spec.indexable()
        );
    }

    private Snapshot snapshot(String id) {
        final Snapshot snapshot = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        final LayerSpec spec = spec(id);
        when(snapshot.absolute().spec()).thenReturn(spec);
        return snapshot;
    }

    private LayerSpec spec(String id) {
        final LayerSpec spec = mock(LayerSpec.class, RETURNS_DEEP_STUBS);
        when(spec.id()).thenReturn(LayerId.fromString(id));
        return spec;
    }
}
