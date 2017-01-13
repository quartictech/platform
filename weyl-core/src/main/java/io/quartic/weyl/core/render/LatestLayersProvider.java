package io.quartic.weyl.core.render;

import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import rx.Observable;

import java.util.Map;
import static io.quartic.common.rx.RxUtilsKt.accumulateMap;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;

public class LatestLayersProvider {
    private final Observable<LayerSnapshotSequence> snapshotSequences;

    public LatestLayersProvider(Observable<LayerSnapshotSequence> snapshotSequences) {
        this.snapshotSequences = snapshotSequences;
    }

    public Observable<Map<LayerId, LayerSnapshotSequence>> latestLayers() {
        return snapshotSequences.compose(accumulateMap(snapshot -> snapshot.spec().id(), snapshot -> snapshot))
                .compose(likeBehavior());
    }
}
