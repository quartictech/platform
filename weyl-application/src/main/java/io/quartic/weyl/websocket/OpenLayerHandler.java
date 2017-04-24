package io.quartic.weyl.websocket;

import io.quartic.common.geojson.FeatureCollection;
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
import static io.quartic.common.rx.RxUtilsKt.accumulateMap;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.feature.FeatureConverter.frontendManipulatorFor;
import static java.util.stream.Collectors.toList;
import static rx.Observable.combineLatest;
import static rx.Observable.empty;

public class OpenLayerHandler implements ClientStatusMessageHandler {
    private final Observable<Map<LayerId, Observable<Snapshot>>> sequenceMap;
    private final FeatureConverter featureConverter;

    public OpenLayerHandler(Observable<LayerSnapshotSequence> snapshotSequences, FeatureConverter featureConverter) {
        // Each item is a map from all current layer IDs to the corresponding snapshot sequence for that layer
        this.sequenceMap = snapshotSequences
                .compose(accumulateMap(seq -> seq.spec().id(), LayerSnapshotSequence::snapshots))
                .compose(likeBehavior());
        this.featureConverter = featureConverter;
    }

    @Override
    public Observable<SocketMessage> call(Observable<ClientStatusMessage> clientStatus) {
        final Observable<List<LayerId>> keys = clientStatus.map(ClientStatusMessage::openLayerIds);

        return combineLatest(keys, sequenceMap, this::collectSequences)
                .distinctUntilChanged()
                .switchMap(Observable::merge)   // switchMap (and thus resubscription) is fine because we only care about absolute state
                .map(this::toMessage);
    }

    private List<Observable<Snapshot>> collectSequences(List<LayerId> ids, Map<LayerId, Observable<Snapshot>> sequenceMap) {
        return copyOf(transform(ids, id -> sequenceMap.getOrDefault(id, empty()))); // empty() to handle invalid IDs gracefully
    }

    private SocketMessage toMessage(Snapshot snapshot) {
        final Layer layer = snapshot.absolute();
        return LayerUpdateMessageImpl.builder()
                .layerId(layer.spec().id())
                .snapshotId(snapshot.id())
                .dynamicSchema(layer.dynamicSchema())
                .stats(layer.stats())
                .featureCollection(featureCollection(layer))
                .build();
    }

    private FeatureCollection featureCollection(Layer layer) {
        final Collection<Feature> features = layer.spec().indexable() ? EMPTY_COLLECTION : layer.features();
        final Stream<Feature> computed = layer.spec().view().compute(features);
        return featureConverter.toGeojson(frontendManipulatorFor(layer.dynamicSchema()), computed.collect(toList()));
    }
}
