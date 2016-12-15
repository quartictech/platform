package io.quartic.weyl.websocket;

import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.websocket.message.LayerInfoImpl;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.LayerListUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;

import java.util.List;
import java.util.Set;

import static io.quartic.common.rx.RxUtils.accumulateSet;
import static java.util.stream.Collectors.toList;

public class LayerListUpdateGenerator implements Observable.Transformer<LayerSnapshotSequence, SocketMessage> {
    @Override
    public Observable<SocketMessage> call(Observable<LayerSnapshotSequence> snapshotSequences) {
        return snapshotSequences
                .compose(accumulateSet(LayerSnapshotSequence::spec))
                .map(this::toMessage);
    }

    private SocketMessage toMessage(Set<LayerSpec> specs) {
        return LayerListUpdateMessageImpl.of(toInfo(specs));
    }

    private List<LayerInfo> toInfo(Set<LayerSpec> accumulated) {
        return accumulated.stream().map(this::toInfo).collect(toList());
    }

    private LayerInfo toInfo(LayerSpec spec) {
        return LayerInfoImpl.of(
                spec.id(),
                spec.metadata(),
                spec.staticSchema(),
                !spec.indexable()
        );
    }
}
