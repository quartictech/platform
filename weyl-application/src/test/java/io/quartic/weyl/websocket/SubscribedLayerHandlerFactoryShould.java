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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SubscribedLayerHandlerFactoryShould {
    private final LayerStore layerStore = mock(LayerStore.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final Consumer<SocketMessage> messageConsumer = mock(Consumer.class);
    private final ClientStatusMessageHandler handler = new SubscribedLayerHandlerFactory(layerStore, converter).create(messageConsumer);

    @Test
    public void subscribe_to_layers() throws Exception {
        final LayerId idA = mock(LayerId.class);
        final LayerId idB = mock(LayerId.class);

        final ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.subscribedLiveLayerIds()).thenReturn(newArrayList(idA, idB));

        handler.onClientStatusMessage(msg);

        verify(layerStore).addSubscriber(eq(idA), any());
        verify(layerStore).addSubscriber(eq(idB), any());
    }

    @Test
    public void send_update_messages_on_layer_change() throws Exception {
        final LayerId id = mock(LayerId.class);

        final ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.subscribedLiveLayerIds()).thenReturn(newArrayList(id));

        final AttributeSchema schema = mock(AttributeSchema.class);
        final Collection<Feature> features = mock(List.class);

        when(layerStore.addSubscriber(any(), any())).then(invocation -> {
            Consumer<LayerState> subscriber = invocation.getArgument(1);
            subscriber.accept(LayerStateImpl.of(schema, features));
            return mock(LayerSubscription.class);
        });

        final FeatureCollection featureCollection = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("foo"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
        when(converter.toGeojson(any())).thenReturn(featureCollection);

        handler.onClientStatusMessage(msg);

        verify(converter).toGeojson(features);
        verify(messageConsumer).accept(LayerUpdateMessageImpl.of(id, schema, featureCollection));
    }
}
