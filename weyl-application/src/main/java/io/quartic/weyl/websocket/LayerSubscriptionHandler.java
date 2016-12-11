package io.quartic.weyl.websocket;

import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
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
        return layerStore.layersForLayerId(layerId)
                .map(layer -> generateUpdateMessage(layerId, layer));
    }

    private SocketMessage generateUpdateMessage(LayerId layerId, Layer layer) {
        final Collection<Feature> features = layer.features();
        Stream<Feature> computed = layer.spec().view().compute(features);
        return LayerUpdateMessageImpl.builder()
                .layerId(layerId)
                .schema(layer.spec().schema())
                .featureCollection(featureConverter.toGeojson(computed.collect(toList())))  // TODO: obviously we never want to do this with large static layers
                .build();
    }
}
