package io.quartic.weyl.websocket;

import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerStateImpl;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Test;
import rx.Subscription;
import rx.observers.TestSubscriber;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static rx.Observable.just;

public class LayerSubscriptionHandlerShould {
    private final LayerStore layerStore = mock(LayerStore.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new LayerSubscriptionHandler(layerStore, converter);

    @Test
    public void subscribe_to_layers() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);

        just(subscriptionMessage(idA, idB))
                .compose(handler)
                .subscribe();

        verify(layerStore).addSubscriber(eq(idA), any());
        verify(layerStore).addSubscriber(eq(idB), any());
    }

    @Test
    public void send_update_messages_when_layer_features_change() throws Exception {
        final LayerId id = mock(LayerId.class);
        final AttributeSchema schema = mock(AttributeSchema.class);
        final Collection<Feature> features = mock(List.class);

        when(layerStore.addSubscriber(any(), any())).then(invocation -> {
            Consumer<LayerState> subscriber = invocation.getArgument(1);
            subscriber.accept(LayerStateImpl.of(schema, features));
            return mock(LayerSubscription.class);
        });

        when(converter.toGeojson(any())).thenReturn(featureCollection());

        TestSubscriber<SocketMessage> sub = TestSubscriber.create();
        just(subscriptionMessage(id))
                .compose(handler)
                .subscribe(sub);

        sub.awaitValueCount(1, 100, MILLISECONDS);
        verify(converter).toGeojson(features);
        assertThat(sub.getOnNextEvents(), contains(LayerUpdateMessageImpl.of(id, schema, featureCollection())));
    }

    @Test
    public void unsubscribe_from_layer_when_no_longer_in_list() throws Exception {
        final LayerSubscription layerSubscription = mock(LayerSubscription.class);

        when(layerStore.addSubscriber(any(), any())).thenReturn(layerSubscription);

        just(subscriptionMessage(mock(LayerId.class)), subscriptionMessage(mock(LayerId.class)))
                .compose(handler)
                .subscribe();

        verify(layerStore).removeSubscriber(layerSubscription);
    }

    @Test
    public void unsubscribe_from_layers_on_downstream_unsubscribe() throws Exception {
        final LayerId id = mock(LayerId.class);
        final LayerSubscription layerSubscription = mock(LayerSubscription.class);

        when(layerStore.addSubscriber(any(), any())).thenReturn(layerSubscription);

        final Subscription subscription = just(subscriptionMessage(id))
                .compose(handler)
                .subscribe();
        subscription.unsubscribe();

        verify(layerStore).removeSubscriber(layerSubscription);
    }

    private ClientStatusMessage subscriptionMessage(LayerId... ids) {
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
