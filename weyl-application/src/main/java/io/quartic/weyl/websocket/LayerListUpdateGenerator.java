package io.quartic.weyl.websocket;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.websocket.message.LayerInfoImpl;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.LayerListUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;
import rx.Observable.Transformer;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static rx.Observable.merge;

public class LayerListUpdateGenerator implements Transformer<LayerSnapshotSequence, SocketMessage> {
    @Override
    public Observable<SocketMessage> call(Observable<LayerSnapshotSequence> snapshotSequences) {
        return snapshotSequences
                .compose(this::extractSpecsForLayersWithFeatures)
                .map(this::toMessage);
    }

    // Track which layers currently have features - this also handles layer-deletion nicely (as deleted layers become empty)
    private Observable<Set<LayerSpec>> extractSpecsForLayersWithFeatures(Observable<LayerSnapshotSequence> sequences) {
        final Set<LayerSpec> initial = newHashSet();
        return merge(sequences.map(LayerSnapshotSequence::snapshots))
                .scan(initial, this::addOrRemoveFromSetBasedOnFeatures)
                .distinctUntilChanged(this::collectIds);
    }

    private Set<LayerSpec> addOrRemoveFromSetBasedOnFeatures(Set<LayerSpec> set, Snapshot s) {
        if (s.absolute().features().isEmpty()) {
            set.remove(s.absolute().spec());
        } else {
            set.add(s.absolute().spec());
        }
        return set;
    }

    private Set<LayerId> collectIds(Set<LayerSpec> set) {
        return set.stream().map(LayerSpec::id).collect(toSet());
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
