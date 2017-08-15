package io.quartic.weyl.websocket;

import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Point;
import io.quartic.common.test.rx.Interceptor;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Diff;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.SnapshotId;
import io.quartic.weyl.core.model.StaticSchema;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.api.LayerUpdateType.APPEND;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class OpenLayerHandlerShould {
    private final TestSubscriber<SocketMessage> sub = TestSubscriber.create();
    private final PublishSubject<ClientStatusMessage> statuses = PublishSubject.create();
    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new OpenLayerHandler(snapshotSequences, converter);
    private final Subscription subscription = statuses.compose(handler).subscribe(sub);

    @Before
    public void before() throws Exception {
        when(converter.toGeojson(any(), any(Collection.class))).thenReturn(featureCollection());
    }

    @Test
    public void subscribe_to_and_unsubscribe_from_layer_based_on_status_message() throws Exception {
        final LayerId id = mock(LayerId.class);
        final Interceptor<Snapshot> interceptor = new Interceptor<>();

        nextSequence(spec(id, true), Observable.<Snapshot>never().compose(interceptor));
        nextStatus(status(id));

        assertThat(interceptor.getSubscribed(), equalTo(true));
        assertThat(interceptor.getUnsubscribed(), equalTo(false));

        nextStatus(status());

        assertThat(interceptor.getUnsubscribed(), equalTo(true));
    }

    @Test
    public void send_update_corresponding_to_snapshot() throws Exception {
        final LayerId id = mock(LayerId.class);
        final LayerSpec spec = spec(id, true);
        final Snapshot snapshot = snapshot(spec);

        nextSequence(spec, just(snapshot));
        nextStatus(status(id));

        completeInputsAndAwait();
        final ArrayList<Feature> expected = newArrayList(snapshot.getAbsolute().getFeatures());
        verify(converter).toGeojson(any(), eq(expected));
        assertThat(sub.getOnNextEvents(), contains(message(id, snapshot)));
    }

    @Test
    public void send_no_updates_when_layer_not_found() throws Exception {
        final LayerId id = mock(LayerId.class);

        nextStatus(status(id));

        completeInputsAndAwait();
        verifyZeroInteractions(converter);
        assertThat(sub.getOnNextEvents(), empty());
    }

    @Test
    public void send_no_features_in_updates_if_layer_is_not_live() throws Exception {
        final LayerId id = mock(LayerId.class);
        final LayerSpec spec = spec(id, false); // Not live

        nextSequence(spec, just(snapshot(spec)));
        nextStatus(status(id));

        completeInputsAndAwait();
        verify(converter).toGeojson(any(), eq(newArrayList()));    // No features converted
    }

    @Test
    public void not_send_updates_if_layer_list_unaffected() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);
        final LayerSpec specA = spec(idA, true);
        final LayerSpec specB = spec(idB, true);

        nextSequence(specA, just(snapshot(specA)));
        nextStatus(status(idA));
        nextStatus(status(idA));
        nextSequence(specB, just(snapshot(specB)));

        completeInputsAndAwait();
        assertThat(sub.getOnNextEvents(), hasSize(1));
    }

    @Test
    public void send_updates_for_multiple_layers() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);
        final LayerSpec specA = spec(idA, true);
        final LayerSpec specB = spec(idB, true);
        final Snapshot snapshotA = snapshot(specA);
        final Snapshot snapshotB = snapshot(specB);

        nextSequence(specA, just(snapshotA));
        nextSequence(specB, just(snapshotB));
        nextStatus(status(idA, idB));

        completeInputsAndAwait();
        assertThat(sub.getOnNextEvents(), containsInAnyOrder(message(idA, snapshotA), message(idB, snapshotB)));
    }

    @Test
    public void unsubscribe_from_layers_on_downstream_unsubscribe() throws Exception {
        final LayerId id = mock(LayerId.class);
        final Interceptor<Snapshot> interceptor = new Interceptor<>();

        nextSequence(spec(id, true), Observable.<Snapshot>never().compose(interceptor));
        nextStatus(status(id));
        subscription.unsubscribe();

        assertThat(interceptor.getUnsubscribed(), equalTo(true));
    }

    private void completeInputsAndAwait() {
        statuses.onCompleted();
        snapshotSequences.onCompleted();
        sub.awaitTerminalEvent();
    }

    private void nextSequence(LayerSpec spec, Observable<Snapshot> snapshots) {
        snapshotSequences.onNext(new LayerSnapshotSequence(spec, snapshots));
    }

    private void nextStatus(ClientStatusMessage status) {
        statuses.onNext(status);
    }

    private Snapshot snapshot(LayerSpec spec) {
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.getSpec()).thenReturn(spec);
        when(layer.getFeatures()).thenReturn(EMPTY_COLLECTION.append(newArrayList(mock(Feature.class), mock(Feature.class))));
        return new Snapshot(new SnapshotId("123"), layer, new Diff(APPEND, emptyList()));
    }

    private LayerSpec spec(LayerId id, boolean live) {
        final LayerSpec spec = mock(LayerSpec.class);
        when(spec.getId()).thenReturn(id);
        when(spec.getView()).thenReturn(IDENTITY_VIEW);
        when(spec.getStaticSchema()).thenReturn(mock(StaticSchema.class));
        when(spec.getIndexable()).thenReturn(!live);
        return spec;
    }

    private ClientStatusMessage status(LayerId... ids) {
        final ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.getOpenLayerIds()).thenReturn(asList(ids));
        return msg;
    }

    private LayerUpdateMessage message(LayerId id, Snapshot snapshot) {
        return new LayerUpdateMessage(
                id,
                new SnapshotId("123"),
                snapshot.getAbsolute().getDynamicSchema(),
                snapshot.getAbsolute().getStats(),
                featureCollection()
        );
    }

    private FeatureCollection featureCollection() {
        return new FeatureCollection(newArrayList(new io.quartic.common.geojson.Feature("foo", new Point(newArrayList(1.0, 2.0)))));
    }
}