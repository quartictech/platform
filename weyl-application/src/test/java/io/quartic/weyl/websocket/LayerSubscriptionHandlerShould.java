package io.quartic.weyl.websocket;

import io.quartic.common.test.rx.ObservableInterceptor;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class LayerSubscriptionHandlerShould {
    private final TestSubscriber<SocketMessage> sub = TestSubscriber.create();
    private final PublishSubject<ClientStatusMessage> statuses = PublishSubject.create();
    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new LayerSubscriptionHandler(snapshotSequences, converter);
    private final Subscription subscription = statuses.compose(handler).subscribe(sub);

    @Before
    public void before() throws Exception {
        when(converter.toGeojson(any())).thenReturn(featureCollection());
    }

    @Test
    public void subscribe_to_and_unsubscribe_from_layer_based_on_status_message() throws Exception {
        final LayerId id = mock(LayerId.class);
        final ObservableInterceptor<Layer> interceptor = ObservableInterceptor.create();

        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(id, interceptor.observable()));
        statuses.onNext(status(id));

        assertThat(interceptor.subscribed(), equalTo(true));
        assertThat(interceptor.unsubscribed(), equalTo(false));

        statuses.onNext(status());

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    @Test
    public void send_update_corresponding_to_snapshot() throws Exception {
        final LayerId id = mock(LayerId.class);
        final Layer snapshot = layer(id);

        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(id, just(snapshot)));
        statuses.onNext(status(id));
        statuses.onCompleted();
        snapshotSequences.onCompleted();

        sub.awaitTerminalEvent();
        verify(converter).toGeojson(newArrayList(snapshot.features()));
        assertThat(sub.getOnNextEvents(), contains(LayerUpdateMessageImpl.of(id, snapshot.spec().schema(), featureCollection())));
    }

    @Test
    public void send_no_updates_when_layer_not_found() throws Exception {
        final LayerId id = mock(LayerId.class);

        statuses.onNext(status(id));
        statuses.onCompleted();
        snapshotSequences.onCompleted();

        sub.awaitTerminalEvent();
        verifyZeroInteractions(converter);
        assertThat(sub.getOnNextEvents(), empty());
    }

    @Test
    public void not_send_updates_if_layer_list_unaffected() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);
        final Layer snapshotA = layer(idA);
        final Layer snapshotB = layer(idB);

        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(idA, just(snapshotA)));
        statuses.onNext(status(idA));
        statuses.onNext(status(idA));                                                       // Duplicate
        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(idB, just(snapshotB)));       // Change to map
        statuses.onCompleted();
        snapshotSequences.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), hasSize(1));
    }

    @Test
    public void send_updates_for_multiple_layers() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);
        final Layer snapshotA = layer(idA);
        final Layer snapshotB = layer(idB);

        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(idA, just(snapshotA)));
        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(idB, just(snapshotB)));
        statuses.onNext(status(idA, idB));
        statuses.onCompleted();
        snapshotSequences.onCompleted();

        sub.awaitTerminalEvent();
        assertThat(sub.getOnNextEvents(), containsInAnyOrder(
                LayerUpdateMessageImpl.of(idA, snapshotA.spec().schema(), featureCollection()),
                LayerUpdateMessageImpl.of(idB, snapshotB.spec().schema(), featureCollection())
        ));
    }

    @Test
    public void unsubscribe_from_layers_on_downstream_unsubscribe() throws Exception {
        final LayerId id = mock(LayerId.class);
        final ObservableInterceptor<Layer> interceptor = ObservableInterceptor.create();

        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(id, interceptor.observable()));
        statuses.onNext(status(id));

        subscription.unsubscribe();

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    private Layer layer(LayerId id) {
        final AttributeSchema schema = mock(AttributeSchema.class);
        final Collection<Feature> features = newArrayList(mock(Feature.class), mock(Feature.class));

        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.spec().id()).thenReturn(id);
        when(layer.spec().view()).thenReturn(IDENTITY_VIEW);
        when(layer.spec().schema()).thenReturn(schema);
        when(layer.features()).thenReturn(EMPTY_COLLECTION.append(features));
        return layer;
    }

    private ClientStatusMessage status(LayerId... ids) {
        final ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.subscribedLiveLayerIds()).thenReturn(asList(ids));
        return msg;
    }

    private FeatureCollection featureCollection() {
        return FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("foo"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
    }
}
