package io.quartic.weyl.websocket;

import io.quartic.common.test.rx.ObservableInterceptor;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.SelectionStatus;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Test;
import rx.Subscription;
import rx.observers.TestSubscriber;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class LayerSubscriptionHandlerShould {
    private final LayerStore store = mock(LayerStore.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new LayerSubscriptionHandler(store, converter);

    @Test
    public void subscribe_to_layers() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);
        final ObservableInterceptor<Layer> interceptorA = ObservableInterceptor.create();
        final ObservableInterceptor<Layer> interceptorB = ObservableInterceptor.create();
        when(store.layer(idA)).thenReturn(interceptorA.observable());
        when(store.layer(idB)).thenReturn(interceptorB.observable());

        just(status(idA, idB))
                .compose(handler)
                .subscribe();

        verify(store).layer(eq(idA));
        verify(store).layer(eq(idB));
        assertThat(interceptorA.subscribed(), equalTo(true));
        assertThat(interceptorB.subscribed(), equalTo(true));
    }

    @Test
    public void send_update_messages_when_layer_changes() throws Exception {
        final LayerId id = mock(LayerId.class);
        final Layer layer = layer();
        when(store.layer(any())).thenReturn(just(layer));
        when(converter.toGeojson(any())).thenReturn(featureCollection());

        TestSubscriber<SocketMessage> sub = TestSubscriber.create();
        just(status(id))
                .compose(handler)
                .subscribe(sub);

        sub.awaitValueCount(1, 100, MILLISECONDS);
        verify(converter).toGeojson(newArrayList(layer.features()));
        assertThat(sub.getOnNextEvents(), contains(LayerUpdateMessageImpl.of(id, layer.spec().schema(), featureCollection())));
    }

    @Test
    public void unsubscribe_from_layer_when_no_longer_in_list() throws Exception {
        final ObservableInterceptor<Layer> interceptor = ObservableInterceptor.create();
        when(store.layer(any())).thenReturn(interceptor.observable());

        just(status(mock(LayerId.class)), status(mock(LayerId.class)))
                .compose(handler)
                .subscribe();

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    @Test
    public void unsubscribe_from_layers_on_downstream_unsubscribe() throws Exception {
        final ObservableInterceptor<Layer> interceptor = ObservableInterceptor.create();
        when(store.layer(any())).thenReturn(interceptor.observable());

        final Subscription subscription = just(status(mock(LayerId.class)))
                .compose(handler)
                .subscribe();
        subscription.unsubscribe();

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    @Test
    public void ignore_status_changes_not_involving_layer_subscription_change() throws Exception {
        final ObservableInterceptor<Layer> interceptor = ObservableInterceptor.create();
        final LayerId id = mock(LayerId.class);
        final ClientStatusMessage statusA = status(id);
        final ClientStatusMessage statusB = status(id);
        when(statusA.selection()).thenReturn(mock(SelectionStatus.class));
        when(statusB.selection()).thenReturn(mock(SelectionStatus.class));  // Different
        when(store.layer(any())).thenReturn(interceptor.observable());

        just(statusA, statusB)
                .compose(handler)
                .subscribe();

        assertThat(interceptor.unsubscribed(), equalTo(false));
    }

    private Layer layer() {
        final AttributeSchema schema = mock(AttributeSchema.class);
        final Collection<Feature> features = newArrayList(mock(Feature.class), mock(Feature.class));

        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
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
