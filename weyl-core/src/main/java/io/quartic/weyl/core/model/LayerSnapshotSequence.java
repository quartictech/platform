package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface LayerSnapshotSequence {
    @SweetStyle
    @Value.Immutable
    interface Snapshot {
        Layer absolute();
        Collection<Feature> diff();
    }

    LayerSpec spec();
    Observable<Snapshot> snapshots();
}
