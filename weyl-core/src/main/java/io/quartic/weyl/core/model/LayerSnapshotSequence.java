package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;
import rx.Observable;

@SweetStyle
@Value.Immutable
public interface LayerSnapshotSequence {
    LayerId id();
    Observable<Layer> snapshots();
}
