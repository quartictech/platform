package io.quartic.weyl.core.source;

import io.quartic.weyl.core.live.LayerViewType;
import rx.Observable;

public interface Source {
    Observable<SourceUpdate> getObservable();
    boolean indexable();
    LayerViewType viewType();
}
