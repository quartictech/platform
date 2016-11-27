package io.quartic.weyl.websocket;

import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;

public class SubscribedLayerHandlerFactory implements ClientStatusMessageHandler.Factory {
    private final LayerStore layerStore;
    private final FeatureConverter featureConverter;

    public SubscribedLayerHandlerFactory(LayerStore layerStore, FeatureConverter featureConverter) {
        this.layerStore = layerStore;
        this.featureConverter = featureConverter;
    }

    @Override
    public ClientStatusMessageHandler create(Consumer<SocketMessage> messageConsumer) {
        final List<LayerSubscription> subscriptions = newArrayList();

        return new ClientStatusMessageHandler() {
            @Override
            public void onClientStatusMessage(ClientStatusMessage msg) {
                unsubscribeAll();
                msg.subscribedLiveLayerIds().forEach(this::subscribe);
            }

            @Override
            public void close() throws Exception {
                unsubscribeAll();
            }

            private void unsubscribeAll() {
                subscriptions.forEach(layerStore::removeSubscriber);
                subscriptions.clear();
            }

            private void subscribe(LayerId layerId) {
                subscriptions.add(layerStore.addSubscriber(layerId, state -> messageConsumer.accept(generateUpdateMessage(layerId, state))));
            }
        };
    }

    private SocketMessage generateUpdateMessage(LayerId layerId, LayerState state) {
        return LayerUpdateMessageImpl.builder()
                .layerId(layerId)
                .schema(state.schema())
                .featureCollection(featureConverter.toGeojson(state.featureCollection()))  // TODO: obviously we never want to do this with large static layers
                .build();
    }
}
