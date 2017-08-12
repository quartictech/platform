package io.quartic.weyl.websocket;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static org.hamcrest.Matchers.contains;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
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
    public void send_messages_on_new_layers_with_features() throws Exception {
        final LayerSpec specFoo = spec("foo");
        final LayerSpec specBar = spec("bar");
        final BehaviorSubject<Snapshot> foo = registerSequence(specFoo);
        final BehaviorSubject<Snapshot> bar = registerSequence(specBar);

        foo.onNext(snapshot(specFoo, 100));
        bar.onNext(snapshot(specBar, 100));
        sequences.onCompleted();
        foo.onCompleted();
        bar.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), contains(
                message(),
                message(layerInfo(specFoo)),
                message(layerInfo(specFoo), layerInfo(specBar))
        ));
    }

    @Test
    public void not_list_empty_layers() throws Exception {
        final LayerSpec spec = spec("foo");
        final BehaviorSubject<Snapshot> foo = registerSequence(spec);

        foo.onNext(snapshot(spec, 100)); // Initially present
        foo.onNext(snapshot(spec, 0));   // Then empty
        sequences.onCompleted();
        foo.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), contains(
                message(),
                message(layerInfo(spec)),
                message()                   // Empty
        ));
    }

    @Test
    public void not_send_messages_on_updates_to_layer_not_affecting_emptiness() throws Exception {
        final LayerSpec specFoo = spec("foo");
        final BehaviorSubject<Snapshot> foo = registerSequence(specFoo);

        foo.onNext(snapshot(specFoo, 100));
        foo.onNext(snapshot(specFoo, 200));
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
        sequences.onNext(new LayerSnapshotSequence(spec, snapshots));
        return snapshots;
    }

    private LayerListUpdateMessage message(LayerInfo... infos) {
        return new LayerListUpdateMessage(asSet(infos));
    }

    private LayerInfo layerInfo(LayerSpec spec) {
        return new LayerInfo(
                spec.getId(),
                spec.getMetadata(),
                spec.getStaticSchema(),
                !spec.getIndexable()
        );
    }

    private Snapshot snapshot(LayerSpec spec, int size) {
        final Snapshot snapshot = mock(Snapshot.class, RETURNS_DEEP_STUBS);
        when(snapshot.getAbsolute().getSpec()).thenReturn(spec);
        when(snapshot.getAbsolute().getFeatures().size()).thenReturn(size);
        when(snapshot.getAbsolute().getFeatures().isEmpty()).thenReturn(size == 0);
        return snapshot;
    }

    private LayerSpec spec(String id) {
        final LayerSpec spec = mock(LayerSpec.class, RETURNS_DEEP_STUBS);
        when(spec.getId()).thenReturn(new LayerId(id));
        return spec;
    }
}
