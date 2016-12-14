package io.quartic.weyl.websocket;

import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.LayerUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static io.quartic.common.rx.RxUtils.accumulateMap;
import static java.util.stream.Collectors.toList;
import static rx.Observable.combineLatest;
import static rx.Observable.empty;

public class LayerSubscriptionHandler implements ClientStatusMessageHandler {
    private final Observable<Map<LayerId, Observable<Snapshot>>> sequenceMap;
    private final FeatureConverter featureConverter;

    public LayerSubscriptionHandler(Observable<LayerSnapshotSequence> snapshotSequences, FeatureConverter featureConverter) {
        // Each item is a map from all current layer IDs to the corresponding snapshot sequence for that layer
        this.sequenceMap = snapshotSequences
                .compose(accumulateMap(LayerSnapshotSequence::id, LayerSnapshotSequence::snapshots))
                .share();
        this.featureConverter = featureConverter;
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        final Observable<List<LayerId>> keys = clientStatus.map(ClientStatusMessage::subscribedLiveLayerIds);

        return combineLatest(keys, sequenceMap, this::collectSequences)
                .distinctUntilChanged()
                .switchMap(Observable::merge)
                .map(this::toMessage);
    }

    private List<Observable<Snapshot>> collectSequences(List<LayerId> ids, Map<LayerId, Observable<Snapshot>> sequenceMap) {
        return copyOf(transform(ids, id -> sequenceMap.getOrDefault(id, empty()))); // empty() to handle invalid IDs gracefully
    }

    private SocketMessage toMessage(Snapshot snapshot) {
        final Layer layer = snapshot.absolute();
        final Collection<Feature> features = layer.features();
        Stream<Feature> computed = layer.spec().view().compute(features);
        return LayerUpdateMessageImpl.builder()
                .layerId(layer.spec().id())
                .schema(layer.spec().schema())
                .featureCollection(featureConverter.toGeojson(computed.collect(toList())))  // TODO: obviously we never want to do this with large static layers
                .build();
    }
}
