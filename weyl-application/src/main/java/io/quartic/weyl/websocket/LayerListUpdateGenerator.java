package io.quartic.weyl.websocket;

import com.google.common.collect.Lists;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.websocket.message.LayerInfoImpl;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.LayerListUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static io.quartic.common.rx.RxUtils.accumulateMap;
import static java.util.Arrays.asList;
import static rx.Observable.combineLatest;
import static rx.Observable.switchOnNext;

public class LayerListUpdateGenerator implements Observable.Transformer<LayerSnapshotSequence, SocketMessage> {
    @Override
    public Observable<SocketMessage> call(Observable<LayerSnapshotSequence> snapshotSequences) {
        //noinspection StaticPseudoFunctionalStyleMethod
        return switchOnNext(
                snapshotSequences
                        .compose(accumulateMap(LayerSnapshotSequence::id, LayerSnapshotSequence::snapshots))
                        .map(accumulated -> combineLatest(
                                transform(
                                        accumulated.values(),
                                        v -> v.map(this::toInfo).distinctUntilChanged()
                                ),
                                this::toMessage
                        ))

        );
    }

    private LayerInfo toInfo(Snapshot snapshot) {
        final Layer layer = snapshot.absolute();
        return LayerInfoImpl.of(
                layer.spec().id(),
                layer.spec().metadata(),
                layer.stats(),
                layer.spec().schema(),
                !layer.spec().indexable()
        );
    }

    private SocketMessage toMessage(Object... args) {
        return LayerListUpdateMessageImpl.of(combine(args));
    }

    @SuppressWarnings("unchecked")
    private static <V> List<V> combine(Object... args) {
        return Lists.transform(asList(args), x -> (V)x);
    }
}
