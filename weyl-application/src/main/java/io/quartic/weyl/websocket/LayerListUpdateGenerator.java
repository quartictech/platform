package io.quartic.weyl.websocket;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage;
import io.quartic.weyl.websocket.message.LayerListUpdateMessage.LayerInfo;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;
import rx.Observable.Transformer;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
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
        return merge(sequences.map(LayerSnapshotSequence::getSnapshots))
                .scan(initial, this::addOrRemoveFromSetBasedOnFeatures)
                .distinctUntilChanged(this::collectIds);
    }

    private Set<LayerSpec> addOrRemoveFromSetBasedOnFeatures(Set<LayerSpec> set, Snapshot s) {
        if (s.getAbsolute().getFeatures().isEmpty()) {
            set.remove(s.getAbsolute().getSpec());
        } else {
            set.add(s.getAbsolute().getSpec());
        }
        return set;
    }

    private Set<LayerId> collectIds(Set<LayerSpec> set) {
        return set.stream().map(LayerSpec::getId).collect(toSet());
    }

    private SocketMessage toMessage(Set<LayerSpec> specs) {
        return new LayerListUpdateMessage(toInfo(specs));
    }

    private Set<LayerInfo> toInfo(Set<LayerSpec> accumulated) {
        return accumulated.stream().map(this::toInfo).collect(toSet());
    }

    private LayerInfo toInfo(LayerSpec spec) {
        return new LayerInfo(
                spec.getId(),
                spec.getMetadata(),
                spec.getStaticSchema(),
                !spec.getIndexable()
        );
    }
}
