package io.quartic.weyl.websocket;

import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Emitter.BackpressureMode;
import rx.Observable;

import static java.util.stream.Collectors.toList;
import static rx.Observable.fromEmitter;
import static rx.Observable.merge;

public class LayerSubscriptionHandler implements ClientStatusMessageHandler {
    private final LayerStore layerStore;
    private final FeatureConverter featureConverter;

    public LayerSubscriptionHandler(LayerStore layerStore, FeatureConverter featureConverter) {
        this.layerStore = layerStore;
        this.featureConverter = featureConverter;
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        return clientStatus
                .map(ClientStatusMessage::subscribedLiveLayerIds)
                .distinctUntilChanged()
                .switchMap(ids -> merge(ids.stream().map(this::upstream).collect(toList())));
    }

    private Observable<SocketMessage> upstream(LayerId layerId) {
        final Observable<LayerState> state = fromEmitter(
                emitter -> {
                    final LayerSubscription layerSubscription = layerStore.addSubscriber(layerId, emitter::onNext);
                    emitter.setCancellation(() -> layerStore.removeSubscriber(layerSubscription));
                },
                BackpressureMode.BUFFER
        );
        return state.map(s -> generateUpdateMessage(layerId, s));
    }

    private SocketMessage generateUpdateMessage(LayerId layerId, LayerState state) {
        return LayerUpdateMessageImpl.builder()
                .layerId(layerId)
                .schema(state.schema())
                .featureCollection(featureConverter.toGeojson(state.featureCollection()))  // TODO: obviously we never want to do this with large static layers
                .build();
    }
}
